package com.gighub.attendance.service.impl;

import com.gighub.attendance.dto.AttendanceScanRequest;
import com.gighub.attendance.mapper.AttendanceScanMapper;
import com.gighub.attendance.mapper.result.AttendanceWorkplaceRow;
import com.gighub.attendance.qr.QrTokenCodec;
import com.gighub.attendance.qr.QrTokenPayload;
import com.gighub.attendance.service.AttendanceScanResult;
import com.gighub.attendance.service.AttendanceScanService;
import com.gighub.attendance.service.command.AttendanceScanCommand;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.ApiTimes;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.document.storage.Sha256;
import com.gighub.idempotency.IdempotencyClaimResult;
import com.gighub.idempotency.IdempotencyClaimService;
import com.gighub.idempotency.IdempotencyKeys;
import com.gighub.member.domain.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HexFormat;

import static com.gighub.attendance.exception.AttendanceException.qrInvalid;
import static com.gighub.attendance.exception.AttendanceException.qrRevoked;
import static com.gighub.attendance.exception.AttendanceException.workplaceLocationRequired;

/** Token·Claim 사전 검증과 Transaction 본 처리의 경계를 관리합니다. */
@Service
public class AttendanceScanServiceImpl implements AttendanceScanService {

    private static final String OPERATION_CODE = "ATTENDANCE_SCAN";
    private static final int MAX_LOCK_ATTEMPTS = 3;
    private static final ZoneId DATABASE_ZONE = ZoneId.of("Asia/Seoul");

    private final QrTokenCodec qrTokenCodec;
    private final AttendanceScanMapper scanMapper;
    private final IdempotencyClaimService claimService;
    private final AttendanceScanExecutor scanExecutor;
    private final AttendanceScanJson scanJson;
    private final Clock clock;

    @Autowired
    public AttendanceScanServiceImpl(
            QrTokenCodec qrTokenCodec,
            AttendanceScanMapper scanMapper,
            IdempotencyClaimService claimService,
            AttendanceScanExecutor scanExecutor,
            AttendanceScanJson scanJson) {
        this(
                qrTokenCodec,
                scanMapper,
                claimService,
                scanExecutor,
                scanJson,
                Clock.system(DATABASE_ZONE));
    }

    /** 요청 판정 시각 경계 테스트에서만 고정 Clock을 주입합니다. */
    AttendanceScanServiceImpl(
            QrTokenCodec qrTokenCodec,
            AttendanceScanMapper scanMapper,
            IdempotencyClaimService claimService,
            AttendanceScanExecutor scanExecutor,
            AttendanceScanJson scanJson,
            Clock clock) {
        this.qrTokenCodec = qrTokenCodec;
        this.scanMapper = scanMapper;
        this.claimService = claimService;
        this.scanExecutor = scanExecutor;
        this.scanJson = scanJson;
        this.clock = clock;
    }

    @Override
    public AttendanceScanResult scan(
            AuthPrincipal principal,
            AttendanceScanRequest request,
            String rawKey) {
        requireWorker(principal);
        IdempotencyKeys.validate(rawKey);

        QrTokenPayload payload = qrTokenCodec.verify(request.getQrToken())
                .orElseThrow(com.gighub.attendance.exception.AttendanceException::qrInvalid);

        Instant attemptedInstant = clock.instant();
        IdempotencyClaimResult claim = claimService.claim(
                principal.getUserId(),
                OPERATION_CODE,
                rawKey,
                fingerprint(request));
        if (claim.isReplay()) {
            return AttendanceScanResult.replayed(
                    scanJson.readResponseBody(claim.getResponseBody()));
        }

        try {
            // 성공 Replay는 QR 재발급 뒤에도 저장 결과를 돌려줘야 하므로 현재 QR 확인은 Claim 뒤에 둡니다.
            AttendanceWorkplaceRow workplace = scanMapper.findWorkplace(payload.workplaceId());
            requireUsableWorkplace(workplace, payload.nonce());

            AttendanceScanCommand command = AttendanceScanCommand.builder()
                    .workerId(principal.getUserId())
                    .workplaceId(payload.workplaceId())
                    .qrNonce(payload.nonce())
                    .latitude(request.getLatitude())
                    .longitude(request.getLongitude())
                    .accuracyMeters(request.getAccuracyMeters())
                    .capturedAt(request.getCapturedAt())
                    .confirmEarlyCheckout(Boolean.TRUE.equals(request.getConfirmEarlyCheckout()))
                    .attemptedAt(ApiTimes.toLocalDateTime(attemptedInstant))
                    .build();
            for (int attempt = 1; attempt <= MAX_LOCK_ATTEMPTS; attempt++) {
                try {
                    return AttendanceScanResult.first(
                            scanExecutor.execute(command, claim.getClaimId()));
                } catch (PessimisticLockingFailureException retryable) {
                    if (attempt == MAX_LOCK_ATTEMPTS) {
                        throw com.gighub.attendance.exception.AttendanceException
                                .temporarilyUnavailable();
                    }
                }
            }
            throw new IllegalStateException("출퇴근 잠금 재시도 횟수가 올바르지 않습니다.");
        } catch (RuntimeException failure) {
            // 본 처리 Transaction이 끝난 뒤 Claim을 지워 같은 Key 재시도를 다시 엽니다.
            claimService.abandon(claim.getClaimId());
            throw failure;
        }
    }

    private void requireWorker(AuthPrincipal principal) {
        if (principal.getRole() != UserRole.WORKER) {
            throw new RoleMismatchException("출퇴근 스캔은 WORKER만 사용할 수 있습니다.");
        }
    }

    private void requireUsableWorkplace(AttendanceWorkplaceRow row, byte[] nonce) {
        if (row == null || !"ACTIVE".equals(row.getStatus())) {
            throw qrInvalid();
        }
        if (row.getQrTokenId() == null
                || row.getTokenNonce() == null
                || !MessageDigest.isEqual(row.getTokenNonce(), nonce)) {
            throw qrRevoked();
        }
        if (row.getLatitude() == null || row.getLongitude() == null) {
            throw workplaceLocationRequired();
        }
    }

    /** Token 원문 대신 Hash와 정규화된 위치 의도만 Claim Fingerprint에 넣습니다. */
    private static byte[] fingerprint(AttendanceScanRequest request) {
        String tokenHash = HexFormat.of().formatHex(
                Sha256.digest(request.getQrToken().getBytes(StandardCharsets.UTF_8)));
        String source = tokenHash + "\n"
                + decimal(request.getLatitude()) + "\n"
                + decimal(request.getLongitude()) + "\n"
                + decimal(request.getAccuracyMeters()) + "\n"
                + request.getCapturedAt() + "\n"
                + Boolean.TRUE.equals(request.getConfirmEarlyCheckout());
        return Sha256.digest(source.getBytes(StandardCharsets.UTF_8));
    }

    private static String decimal(BigDecimal value) {
        BigDecimal normalized = value.stripTrailingZeros();
        return normalized.signum() == 0 ? "0" : normalized.toPlainString();
    }
}
