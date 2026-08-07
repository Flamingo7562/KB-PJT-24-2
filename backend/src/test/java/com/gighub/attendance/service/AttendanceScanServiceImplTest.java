package com.gighub.attendance.service.impl;

import com.gighub.attendance.dto.AttendanceRecordedResponse;
import com.gighub.attendance.dto.AttendanceScanRequest;
import com.gighub.attendance.exception.AttendanceException;
import com.gighub.attendance.mapper.AttendanceScanMapper;
import com.gighub.attendance.mapper.result.AttendanceWorkplaceRow;
import com.gighub.attendance.qr.QrHmacKeys;
import com.gighub.attendance.qr.QrTokenCodec;
import com.gighub.attendance.service.AttendanceScanResult;
import com.gighub.attendance.service.command.AttendanceScanCommand;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.idempotency.IdempotencyClaimResult;
import com.gighub.idempotency.IdempotencyClaimService;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.CannotAcquireLockException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AttendanceScanServiceImplTest {

    private static final long WORKPLACE_ID = 7L;
    private static final long WORKER_ID = 9L;
    private static final String KEY = "ATTENDANCE-SCAN-KEY-001";
    private static final byte[] NONCE = new byte[QrTokenCodec.NONCE_LENGTH];
    private static final Instant NOW = Instant.parse("2026-08-07T01:00:00Z");

    private final AttendanceScanMapper scanMapper = mock(AttendanceScanMapper.class);
    private final IdempotencyClaimService claimService = mock(IdempotencyClaimService.class);
    private final AttendanceScanExecutor scanExecutor = mock(AttendanceScanExecutor.class);
    private final AttendanceScanJson scanJson = mock(AttendanceScanJson.class);
    private final QrTokenCodec tokenCodec = new QrTokenCodec(new QrHmacKeys(
            "test",
            Map.of("test", new byte[32])));
    private final AttendanceScanServiceImpl service = new AttendanceScanServiceImpl(
            tokenCodec,
            scanMapper,
            claimService,
            scanExecutor,
            scanJson,
            Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));

    private AttendanceScanRequest request;
    private String token;

    @BeforeEach
    void setUp() {
        token = tokenCodec.sign(WORKPLACE_ID, NONCE);
        request = mock(AttendanceScanRequest.class);
        when(request.getQrToken()).thenReturn(token);
        when(request.getLatitude()).thenReturn(new BigDecimal("37.1234567"));
        when(request.getLongitude()).thenReturn(new BigDecimal("127.1234567"));
        when(request.getAccuracyMeters()).thenReturn(new BigDecimal("12.30"));
        when(request.getCapturedAt()).thenReturn(NOW);
        when(request.getConfirmEarlyCheckout()).thenReturn(false);
    }

    @Test
    void completedClaimReplaysBeforeCurrentQrLookup() {
        AttendanceRecordedResponse stored = recorded();
        when(claimService.claim(eq(WORKER_ID), eq("ATTENDANCE_SCAN"), eq(KEY), any(byte[].class)))
                .thenReturn(IdempotencyClaimResult.replay(200, "stored"));
        when(scanJson.readResponseBody("stored")).thenReturn(stored);

        AttendanceScanResult result = service.scan(worker(), request, KEY);

        assertTrue(result.isReplayed());
        assertEquals(stored, result.getResponse());
        verifyNoInteractions(scanMapper, scanExecutor);
    }

    @Test
    void currentQrFailureAfterClaimAbandonsTheClaim() {
        when(claimService.claim(eq(WORKER_ID), eq("ATTENDANCE_SCAN"), eq(KEY), any(byte[].class)))
                .thenReturn(IdempotencyClaimResult.started(31L));

        assertThrows(AttendanceException.class, () -> service.scan(worker(), request, KEY));

        verify(claimService).abandon(31L);
        verify(scanExecutor, never()).execute(any(), anyLong());
    }

    @Test
    void firstRequestUsesServerAttemptedAtAndCompletesThroughExecutor() {
        when(claimService.claim(eq(WORKER_ID), eq("ATTENDANCE_SCAN"), eq(KEY), any(byte[].class)))
                .thenReturn(IdempotencyClaimResult.started(32L));
        when(scanMapper.findWorkplace(WORKPLACE_ID)).thenReturn(workplace());
        when(scanExecutor.execute(any(AttendanceScanCommand.class), eq(32L))).thenReturn(recorded());

        AttendanceScanResult result = service.scan(worker(), request, KEY);

        ArgumentCaptor<AttendanceScanCommand> command =
                ArgumentCaptor.forClass(AttendanceScanCommand.class);
        verify(scanExecutor).execute(command.capture(), eq(32L));
        assertEquals(LocalDateTime.of(2026, 8, 7, 10, 0), command.getValue().getAttemptedAt());
        assertEquals(WORKER_ID, command.getValue().getWorkerId());
        assertEquals(false, result.isReplayed());
        verify(claimService, never()).abandon(anyLong());
    }

    @Test
    void lockConflictRetriesWithTheSameClaim() {
        when(claimService.claim(eq(WORKER_ID), eq("ATTENDANCE_SCAN"), eq(KEY), any(byte[].class)))
                .thenReturn(IdempotencyClaimResult.started(33L));
        when(scanMapper.findWorkplace(WORKPLACE_ID)).thenReturn(workplace());
        when(scanExecutor.execute(any(AttendanceScanCommand.class), eq(33L)))
                .thenThrow(new CannotAcquireLockException("lock"))
                .thenReturn(recorded());

        AttendanceScanResult result = service.scan(worker(), request, KEY);

        assertEquals(false, result.isReplayed());
        verify(scanExecutor, times(2)).execute(any(AttendanceScanCommand.class), eq(33L));
        verify(claimService, never()).abandon(anyLong());
    }

    @Test
    void exhaustedLockRetriesReturnServiceUnavailableAndAbandonClaim() {
        when(claimService.claim(eq(WORKER_ID), eq("ATTENDANCE_SCAN"), eq(KEY), any(byte[].class)))
                .thenReturn(IdempotencyClaimResult.started(34L));
        when(scanMapper.findWorkplace(WORKPLACE_ID)).thenReturn(workplace());
        when(scanExecutor.execute(any(AttendanceScanCommand.class), eq(34L)))
                .thenThrow(new CannotAcquireLockException("lock"));

        AttendanceException failure = assertThrows(
                AttendanceException.class,
                () -> service.scan(worker(), request, KEY));

        assertEquals(
                com.gighub.common.api.ApiErrorCode.ATTENDANCE_TEMPORARILY_UNAVAILABLE,
                failure.getCode());
        verify(scanExecutor, times(3)).execute(any(AttendanceScanCommand.class), eq(34L));
        verify(claimService).abandon(34L);
    }

    private AuthPrincipal worker() {
        return new AuthPrincipal(WORKER_ID, UserRole.WORKER, "근로자");
    }

    private AttendanceWorkplaceRow workplace() {
        AttendanceWorkplaceRow row = new AttendanceWorkplaceRow();
        row.setWorkplaceId(WORKPLACE_ID);
        row.setStatus("ACTIVE");
        row.setLatitude(new BigDecimal("37.1234567"));
        row.setLongitude(new BigDecimal("127.1234567"));
        row.setRadiusMeters(new BigDecimal("100.00"));
        row.setQrTokenId(11L);
        row.setTokenNonce(NONCE);
        return row;
    }

    private AttendanceRecordedResponse recorded() {
        return new AttendanceRecordedResponse(
                101L, "CHECK_IN", NOW, false, 0, null, null);
    }
}
