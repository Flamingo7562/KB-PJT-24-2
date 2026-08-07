package com.gighub.attendance.service.impl;

import com.gighub.attendance.dto.AttendanceConfirmationRequiredResponse;
import com.gighub.attendance.dto.AttendanceRecordedResponse;
import com.gighub.attendance.dto.AttendanceScanResponse;
import com.gighub.attendance.exception.AttendanceException;
import com.gighub.attendance.mapper.AttendanceScanMapper;
import com.gighub.attendance.mapper.param.AttendanceRecordInsertParam;
import com.gighub.attendance.mapper.result.AttendanceCandidateRow;
import com.gighub.attendance.mapper.result.AttendanceScanWorkCaseRow;
import com.gighub.attendance.mapper.result.AttendanceWorkplaceRow;
import com.gighub.attendance.service.command.AttendanceScanCommand;
import com.gighub.common.api.ApiTimes;
import com.gighub.idempotency.IdempotencyClaimService;
import com.gighub.settlement.mapper.SettlementMapper;
import com.gighub.work.domain.WorkCaseStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/** 사업장과 근무를 잠근 뒤 근태·상태·정산 예약을 한 Transaction으로 확정합니다. */
@Component
public class AttendanceScanExecutor {

    private static final String CHECK_IN = "CHECK_IN";
    private static final String CHECK_OUT = "CHECK_OUT";
    private static final String SUCCESS = "SUCCESS";
    private static final String REJECTED = "REJECTED";
    private static final double EARTH_RADIUS_METERS = 6_371_000D;

    private final AttendanceScanMapper scanMapper;
    private final SettlementMapper settlementMapper;
    private final IdempotencyClaimService claimService;
    private final AttendanceScanJson scanJson;

    public AttendanceScanExecutor(
            AttendanceScanMapper scanMapper,
            SettlementMapper settlementMapper,
            IdempotencyClaimService claimService,
            AttendanceScanJson scanJson) {
        this.scanMapper = scanMapper;
        this.settlementMapper = settlementMapper;
        this.claimService = claimService;
        this.scanJson = scanJson;
    }

    @Transactional(noRollbackFor = AttendanceException.class)
    public AttendanceScanResponse execute(AttendanceScanCommand command, long claimId) {
        AttendanceWorkplaceRow workplace = lockAndVerifyWorkplace(command);
        AttendanceCandidateRow candidate = findSingleCandidate(command);
        AttendanceScanWorkCaseRow workCase = scanMapper.lockWorkCase(candidate.getWorkCaseId());
        requireStillEligible(command, candidate, workCase, workplace);

        requireFreshLocation(command, candidate, workCase, workplace);
        double calculatedDistance = distance(command, workplace);
        BigDecimal storedDistance = BigDecimal.valueOf(calculatedDistance)
                .setScale(2, RoundingMode.HALF_UP);
        // 반경 판정은 저장용 소수점 둘째 자리 반올림 전에 끝내야 100.004m가 100.00m로 통과하지 않습니다.
        if (calculatedDistance > workplace.getRadiusMeters().doubleValue()) {
            reject(
                    command,
                    candidate,
                    workCase,
                    workplace,
                    storedDistance,
                    AttendanceException.outsideRadius());
        }

        AttendanceScanResponse response = CHECK_IN.equals(candidate.getScanType())
                ? recordCheckIn(command, workCase, workplace, storedDistance)
                : recordCheckOut(command, workCase, workplace, storedDistance);
        claimService.complete(claimId, 200, scanJson.writeResponseBody(response));
        return response;
    }

    private AttendanceWorkplaceRow lockAndVerifyWorkplace(AttendanceScanCommand command) {
        AttendanceWorkplaceRow row = scanMapper.lockWorkplace(command.getWorkplaceId());
        if (row == null || !"ACTIVE".equals(row.getStatus())) {
            throw AttendanceException.qrInvalid();
        }
        if (row.getQrTokenId() == null
                || row.getTokenNonce() == null
                || !MessageDigest.isEqual(row.getTokenNonce(), command.getQrNonce())) {
            throw AttendanceException.qrRevoked();
        }
        if (row.getLatitude() == null || row.getLongitude() == null) {
            throw AttendanceException.workplaceLocationRequired();
        }
        return row;
    }

    private AttendanceCandidateRow findSingleCandidate(AttendanceScanCommand command) {
        LocalDateTime attemptedAt = command.getAttemptedAt();
        List<AttendanceCandidateRow> candidates = scanMapper.findCandidates(
                command.getWorkerId(),
                command.getWorkplaceId(),
                attemptedAt.plusMinutes(30),
                attemptedAt.minusHours(1),
                attemptedAt.minusHours(2));
        if (candidates.size() > 1) {
            throw AttendanceException.workCaseAmbiguous();
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        List<Long> completed = scanMapper.findCompletedCandidateIds(
                command.getWorkerId(),
                command.getWorkplaceId(),
                attemptedAt.plusMinutes(30),
                attemptedAt.minusHours(2));
        if (completed.size() == 1) {
            throw AttendanceException.alreadyCompleted();
        }
        throw AttendanceException.workCaseNotFound();
    }

    private void requireStillEligible(
            AttendanceScanCommand command,
            AttendanceCandidateRow candidate,
            AttendanceScanWorkCaseRow workCase,
            AttendanceWorkplaceRow workplace) {
        if (workCase == null
                || !workCase.getWorkerId().equals(command.getWorkerId())
                || !workCase.getWorkplaceId().equals(command.getWorkplaceId())) {
            throw AttendanceException.workCaseNotFound();
        }

        LocalDateTime attemptedAt = command.getAttemptedAt();
        if (CHECK_IN.equals(candidate.getScanType())) {
            boolean timeOpen = !attemptedAt.isBefore(workCase.getStartsAt().minusMinutes(30))
                    && attemptedAt.isBefore(workCase.getStartsAt().plusHours(1));
            if (!timeOpen) {
                reject(
                        command,
                        candidate,
                        workCase,
                        workplace,
                        null,
                        AttendanceException.stateConflict("TIME_WINDOW_CLOSED"));
            }
            if (workCase.getStatus() != WorkCaseStatus.READY
                    || scanMapper.hasSuccessfulAttendance(workCase.getWorkCaseId(), CHECK_IN)) {
                rejectStateConflict(command, candidate, workCase, workplace);
            }
            return;
        }

        boolean timeOpen = attemptedAt.isBefore(workCase.getEndsAt().plusHours(2));
        if (!timeOpen) {
            reject(
                    command,
                    candidate,
                    workCase,
                    workplace,
                    null,
                    AttendanceException.stateConflict("TIME_WINDOW_CLOSED"));
        }
        if (workCase.getStatus() != WorkCaseStatus.IN_PROGRESS
                || !scanMapper.hasSuccessfulAttendance(workCase.getWorkCaseId(), CHECK_IN)
                || scanMapper.hasSuccessfulAttendance(workCase.getWorkCaseId(), CHECK_OUT)) {
            rejectStateConflict(command, candidate, workCase, workplace);
        }
    }

    private void requireFreshLocation(
            AttendanceScanCommand command,
            AttendanceCandidateRow candidate,
            AttendanceScanWorkCaseRow workCase,
            AttendanceWorkplaceRow workplace) {
        Instant attempted = ApiTimes.toInstant(command.getAttemptedAt());
        if (command.getCapturedAt().isBefore(attempted.minus(Duration.ofMinutes(5)))
                || command.getCapturedAt().isAfter(attempted.plus(Duration.ofMinutes(1)))) {
            reject(
                    command,
                    candidate,
                    workCase,
                    workplace,
                    null,
                    AttendanceException.locationInvalid("LOCATION_STALE"));
        }
        if (command.getAccuracyMeters().compareTo(new BigDecimal("100")) > 0) {
            reject(
                    command,
                    candidate,
                    workCase,
                    workplace,
                    null,
                    AttendanceException.locationInvalid("LOCATION_INACCURATE"));
        }
    }

    private AttendanceScanResponse recordCheckIn(
            AttendanceScanCommand command,
            AttendanceScanWorkCaseRow workCase,
            AttendanceWorkplaceRow workplace,
            BigDecimal distance) {
        insert(command, workCase, workplace, CHECK_IN, SUCCESS, null, null, distance);
        if (scanMapper.transitionStatus(
                workCase.getWorkCaseId(),
                WorkCaseStatus.READY.name(),
                WorkCaseStatus.IN_PROGRESS.name()) != 1) {
            throw new IllegalStateException("출근 근무 상태를 전이하지 못했습니다.");
        }

        boolean late = command.getAttemptedAt().isAfter(workCase.getStartsAt());
        int lateMinutes = late
                ? ceilMinutes(Duration.between(workCase.getStartsAt(), command.getAttemptedAt()))
                : 0;
        return new AttendanceRecordedResponse(
                workCase.getWorkCaseId(),
                CHECK_IN,
                ApiTimes.toInstant(command.getAttemptedAt()),
                late,
                lateMinutes,
                null,
                null);
    }

    private AttendanceScanResponse recordCheckOut(
            AttendanceScanCommand command,
            AttendanceScanWorkCaseRow workCase,
            AttendanceWorkplaceRow workplace,
            BigDecimal distance) {
        boolean early = command.getAttemptedAt().isBefore(workCase.getEndsAt());
        if (early && !command.isConfirmEarlyCheckout()) {
            return new AttendanceConfirmationRequiredResponse(
                    workCase.getWorkCaseId(), ApiTimes.toInstant(workCase.getEndsAt()));
        }

        LocalDateTime confirmedAt = early ? command.getAttemptedAt() : null;
        insert(command, workCase, workplace, CHECK_OUT, SUCCESS, null, confirmedAt, distance);
        if (scanMapper.transitionStatus(
                workCase.getWorkCaseId(),
                WorkCaseStatus.IN_PROGRESS.name(),
                WorkCaseStatus.COMPLETED.name()) != 1) {
            throw new IllegalStateException("퇴근 근무 상태를 전이하지 못했습니다.");
        }

        LocalDateTime dueAt = command.getAttemptedAt().plusHours(24);
        if (settlementMapper.scheduleWaiting(workCase.getWorkCaseId(), dueAt) != 1) {
            throw new IllegalStateException("정상 퇴근 정산을 예약하지 못했습니다.");
        }
        return new AttendanceRecordedResponse(
                workCase.getWorkCaseId(),
                CHECK_OUT,
                ApiTimes.toInstant(command.getAttemptedAt()),
                false,
                0,
                ApiTimes.toInstant(confirmedAt),
                ApiTimes.toInstant(dueAt));
    }

    private void rejectStateConflict(
            AttendanceScanCommand command,
            AttendanceCandidateRow candidate,
            AttendanceScanWorkCaseRow workCase,
            AttendanceWorkplaceRow workplace) {
        reject(
                command,
                candidate,
                workCase,
                workplace,
                null,
                AttendanceException.stateConflict("STATE_CONFLICT"));
    }

    private void reject(
            AttendanceScanCommand command,
            AttendanceCandidateRow candidate,
            AttendanceScanWorkCaseRow workCase,
            AttendanceWorkplaceRow workplace,
            BigDecimal distance,
            AttendanceException failure) {
        insert(
                command,
                workCase,
                workplace,
                candidate.getScanType(),
                REJECTED,
                failure.getFailureReason(),
                null,
                distance);
        throw failure;
    }

    private void insert(
            AttendanceScanCommand command,
            AttendanceScanWorkCaseRow workCase,
            AttendanceWorkplaceRow workplace,
            String attendanceType,
            String result,
            String failureReason,
            LocalDateTime confirmedAt,
            BigDecimal distance) {
        AttendanceRecordInsertParam param = AttendanceRecordInsertParam.builder()
                .workCaseId(workCase.getWorkCaseId())
                .workerId(command.getWorkerId())
                .qrTokenId(workplace.getQrTokenId())
                .attendanceType(attendanceType)
                .capturedAt(ApiTimes.toLocalDateTime(command.getCapturedAt()))
                .attemptedAt(command.getAttemptedAt())
                .distanceMeters(distance)
                .accuracyMeters(command.getAccuracyMeters())
                .result(result)
                .failureReason(failureReason)
                .earlyCheckoutConfirmedAt(confirmedAt)
                .build();
        if (scanMapper.insertRecord(param) != 1) {
            throw new IllegalStateException("근태 기록을 저장하지 못했습니다.");
        }
    }

    private static double distance(
            AttendanceScanCommand command,
            AttendanceWorkplaceRow workplace) {
        double latitude1 = Math.toRadians(command.getLatitude().doubleValue());
        double latitude2 = Math.toRadians(workplace.getLatitude().doubleValue());
        double latitudeDelta = latitude2 - latitude1;
        double longitudeDelta = Math.toRadians(
                workplace.getLongitude().doubleValue() - command.getLongitude().doubleValue());
        double haversine = Math.pow(Math.sin(latitudeDelta / 2D), 2D)
                + Math.cos(latitude1) * Math.cos(latitude2)
                * Math.pow(Math.sin(longitudeDelta / 2D), 2D);
        double boundedHaversine = Math.min(1D, Math.max(0D, haversine));
        return 2D * EARTH_RADIUS_METERS
                * Math.atan2(
                        Math.sqrt(boundedHaversine),
                        Math.sqrt(1D - boundedHaversine));
    }

    private static int ceilMinutes(Duration duration) {
        long wholeMinutes = duration.toMinutes();
        long roundedMinutes = duration.minusMinutes(wholeMinutes).isZero()
                ? wholeMinutes
                : wholeMinutes + 1L;
        return Math.toIntExact(roundedMinutes);
    }
}
