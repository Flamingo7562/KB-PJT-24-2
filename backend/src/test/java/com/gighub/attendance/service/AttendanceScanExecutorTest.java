package com.gighub.attendance.service;

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
import com.gighub.attendance.service.impl.AttendanceScanExecutor;
import com.gighub.attendance.service.impl.AttendanceScanJson;
import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.api.ApiTimes;
import com.gighub.idempotency.IdempotencyClaimService;
import com.gighub.settlement.mapper.SettlementMapper;
import com.gighub.work.domain.WorkCaseStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceScanExecutorTest {

    private static final long WORKER_ID = 7L;
    private static final long WORKPLACE_ID = 11L;
    private static final long WORK_CASE_ID = 17L;
    private static final long QR_TOKEN_ID = 23L;
    private static final long CLAIM_ID = 29L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 7, 9, 0);
    private static final byte[] NONCE = new byte[16];

    @Mock
    private AttendanceScanMapper scanMapper;

    @Mock
    private SettlementMapper settlementMapper;

    @Mock
    private IdempotencyClaimService claimService;

    @Mock
    private AttendanceScanJson scanJson;

    @Test
    void recordsCheckInAtReadyBoundaryAndDerivesNoLateness() {
        stubCandidate("CHECK_IN", WorkCaseStatus.READY, NOW.plusMinutes(30), NOW.plusHours(8));
        when(scanMapper.insertRecord(any())).thenReturn(1);
        when(scanMapper.transitionStatus(
                WORK_CASE_ID, WorkCaseStatus.READY.name(), WorkCaseStatus.IN_PROGRESS.name()))
                .thenReturn(1);
        when(scanJson.writeResponseBody(any())).thenReturn("{\"data\":{}}");

        AttendanceScanResponse response = executor().execute(command(false), CLAIM_ID);

        AttendanceRecordedResponse recorded = assertInstanceOf(
                AttendanceRecordedResponse.class, response);
        assertEquals("CHECK_IN", recorded.getScanType());
        assertEquals(false, recorded.getIsLate());
        assertEquals(0, recorded.getLateMinutes());
        verify(claimService).complete(CLAIM_ID, 200, "{\"data\":{}}");
    }

    @Test
    void roundsAnyPositiveCheckInDelayUpToOneMinute() {
        stubCandidate(
                "CHECK_IN",
                WorkCaseStatus.READY,
                NOW.minusNanos(1),
                NOW.plusHours(8));
        when(scanMapper.insertRecord(any())).thenReturn(1);
        when(scanMapper.transitionStatus(
                WORK_CASE_ID, WorkCaseStatus.READY.name(), WorkCaseStatus.IN_PROGRESS.name()))
                .thenReturn(1);
        when(scanJson.writeResponseBody(any())).thenReturn("{\"data\":{}}");

        AttendanceRecordedResponse recorded = assertInstanceOf(
                AttendanceRecordedResponse.class,
                executor().execute(command(false), CLAIM_ID));

        assertEquals(true, recorded.getIsLate());
        assertEquals(1, recorded.getLateMinutes());
    }

    @Test
    void asksForConfirmationWithoutWritingAttendanceBeforeScheduledEnd() {
        stubCandidate("CHECK_OUT", WorkCaseStatus.IN_PROGRESS, NOW.minusHours(7), NOW.plusHours(1));
        when(scanMapper.hasSuccessfulAttendance(WORK_CASE_ID, "CHECK_IN")).thenReturn(true);
        when(scanJson.writeResponseBody(any())).thenReturn("{\"data\":{}}");

        AttendanceScanResponse response = executor().execute(command(false), CLAIM_ID);

        assertInstanceOf(AttendanceConfirmationRequiredResponse.class, response);
        verify(scanMapper, never()).insertRecord(any());
        verify(scanMapper, never()).transitionStatus(anyLong(), anyString(), anyString());
        verify(settlementMapper, never()).scheduleWaiting(anyLong(), any());
        verify(claimService).complete(CLAIM_ID, 200, "{\"data\":{}}");
    }

    @Test
    void completesCheckoutAndSchedulesSettlementForExactlyTwentyFourHoursLater() {
        stubCandidate("CHECK_OUT", WorkCaseStatus.IN_PROGRESS, NOW.minusHours(8), NOW);
        when(scanMapper.hasSuccessfulAttendance(WORK_CASE_ID, "CHECK_IN")).thenReturn(true);
        when(scanMapper.insertRecord(any())).thenReturn(1);
        when(scanMapper.transitionStatus(
                WORK_CASE_ID,
                WorkCaseStatus.IN_PROGRESS.name(),
                WorkCaseStatus.COMPLETED.name()))
                .thenReturn(1);
        when(settlementMapper.scheduleWaiting(WORK_CASE_ID, NOW.plusHours(24))).thenReturn(1);
        when(scanJson.writeResponseBody(any())).thenReturn("{\"data\":{}}");

        AttendanceRecordedResponse response = assertInstanceOf(
                AttendanceRecordedResponse.class,
                executor().execute(command(false), CLAIM_ID));

        assertEquals("CHECK_OUT", response.getScanType());
        assertEquals(ApiTimes.toInstant(NOW.plusHours(24)), response.getSettlementDueAt());
        verify(settlementMapper).scheduleWaiting(WORK_CASE_ID, NOW.plusHours(24));
    }

    @Test
    void rejectsUnroundedDistanceOverRadiusAndStoresRoundedAuditDistance() {
        stubCandidate("CHECK_IN", WorkCaseStatus.READY, NOW, NOW.plusHours(8));
        double latitudeDelta = Math.toDegrees(100.004D / 6_371_000D);
        AttendanceWorkplaceRow workplace = workplace();
        workplace.setLatitude(BigDecimal.valueOf(latitudeDelta));
        workplace.setLongitude(BigDecimal.ZERO);
        workplace.setRadiusMeters(new BigDecimal("100.00"));
        when(scanMapper.lockWorkplace(WORKPLACE_ID)).thenReturn(workplace);
        when(scanMapper.insertRecord(any())).thenReturn(1);

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> executor().execute(
                        commandAt(BigDecimal.ZERO, BigDecimal.ZERO, false), CLAIM_ID));

        assertEquals(ApiErrorCode.OUTSIDE_WORKPLACE_RADIUS, failure.getCode());
        ArgumentCaptor<AttendanceRecordInsertParam> captor =
                ArgumentCaptor.forClass(AttendanceRecordInsertParam.class);
        verify(scanMapper).insertRecord(captor.capture());
        assertEquals("REJECTED", captor.getValue().getResult());
        assertEquals("OUTSIDE_RADIUS", captor.getValue().getFailureReason());
        assertEquals(0, new BigDecimal("100.00").compareTo(captor.getValue().getDistanceMeters()));
        verify(claimService, never()).complete(anyLong(), anyInt(), anyString());
    }

    @Test
    void recordsStaleLocationAsRejectedBeforeReturningApprovedError() {
        stubCandidate("CHECK_IN", WorkCaseStatus.READY, NOW, NOW.plusHours(8));
        when(scanMapper.insertRecord(any())).thenReturn(1);
        AttendanceScanCommand stale = AttendanceScanCommand.builder()
                .workerId(WORKER_ID)
                .workplaceId(WORKPLACE_ID)
                .qrNonce(NONCE)
                .latitude(new BigDecimal("37.0"))
                .longitude(new BigDecimal("127.0"))
                .accuracyMeters(new BigDecimal("10.0"))
                .capturedAt(ApiTimes.toInstant(NOW.minusMinutes(5).minusNanos(1)))
                .confirmEarlyCheckout(false)
                .attemptedAt(NOW)
                .build();

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> executor().execute(stale, CLAIM_ID));

        assertEquals(ApiErrorCode.LOCATION_INVALID, failure.getCode());
        ArgumentCaptor<AttendanceRecordInsertParam> captor =
                ArgumentCaptor.forClass(AttendanceRecordInsertParam.class);
        verify(scanMapper).insertRecord(captor.capture());
        assertEquals("LOCATION_STALE", captor.getValue().getFailureReason());
    }

    @Test
    void reportsAmbiguousCandidateWithoutWritingAnAuditRow() {
        when(scanMapper.lockWorkplace(WORKPLACE_ID)).thenReturn(workplace());
        when(scanMapper.findCandidates(
                WORKER_ID,
                WORKPLACE_ID,
                NOW.plusMinutes(30),
                NOW.minusHours(1),
                NOW.minusHours(2)))
                .thenReturn(List.of(candidate("CHECK_IN"), candidate("CHECK_OUT")));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> executor().execute(command(false), CLAIM_ID));

        assertEquals(ApiErrorCode.ATTENDANCE_WORK_CASE_AMBIGUOUS, failure.getCode());
        verify(scanMapper, never()).insertRecord(any());
    }

    private AttendanceScanExecutor executor() {
        return new AttendanceScanExecutor(scanMapper, settlementMapper, claimService, scanJson);
    }

    private void stubCandidate(
            String scanType,
            WorkCaseStatus status,
            LocalDateTime startsAt,
            LocalDateTime endsAt) {
        when(scanMapper.lockWorkplace(WORKPLACE_ID)).thenReturn(workplace());
        when(scanMapper.findCandidates(
                WORKER_ID,
                WORKPLACE_ID,
                NOW.plusMinutes(30),
                NOW.minusHours(1),
                NOW.minusHours(2)))
                .thenReturn(List.of(candidate(scanType)));
        when(scanMapper.lockWorkCase(WORK_CASE_ID))
                .thenReturn(workCase(status, startsAt, endsAt));
    }

    private AttendanceWorkplaceRow workplace() {
        AttendanceWorkplaceRow row = new AttendanceWorkplaceRow();
        row.setWorkplaceId(WORKPLACE_ID);
        row.setStatus("ACTIVE");
        row.setLatitude(new BigDecimal("37.0"));
        row.setLongitude(new BigDecimal("127.0"));
        row.setRadiusMeters(new BigDecimal("100.00"));
        row.setQrTokenId(QR_TOKEN_ID);
        row.setTokenNonce(NONCE);
        return row;
    }

    private AttendanceCandidateRow candidate(String scanType) {
        AttendanceCandidateRow row = new AttendanceCandidateRow();
        row.setWorkCaseId(WORK_CASE_ID);
        row.setScanType(scanType);
        return row;
    }

    private AttendanceScanWorkCaseRow workCase(
            WorkCaseStatus status,
            LocalDateTime startsAt,
            LocalDateTime endsAt) {
        AttendanceScanWorkCaseRow row = new AttendanceScanWorkCaseRow();
        row.setWorkCaseId(WORK_CASE_ID);
        row.setWorkerId(WORKER_ID);
        row.setWorkplaceId(WORKPLACE_ID);
        row.setStatus(status);
        row.setStartsAt(startsAt);
        row.setEndsAt(endsAt);
        return row;
    }

    private AttendanceScanCommand command(boolean confirmEarlyCheckout) {
        return commandAt(new BigDecimal("37.0"), new BigDecimal("127.0"), confirmEarlyCheckout);
    }

    private AttendanceScanCommand commandAt(
            BigDecimal latitude,
            BigDecimal longitude,
            boolean confirmEarlyCheckout) {
        return AttendanceScanCommand.builder()
                .workerId(WORKER_ID)
                .workplaceId(WORKPLACE_ID)
                .qrNonce(NONCE)
                .latitude(latitude)
                .longitude(longitude)
                .accuracyMeters(new BigDecimal("10.0"))
                .capturedAt(ApiTimes.toInstant(NOW))
                .confirmEarlyCheckout(confirmEarlyCheckout)
                .attemptedAt(NOW)
                .build();
    }
}
