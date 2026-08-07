package com.gighub.attendance.service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import com.gighub.attendance.dto.WorkplaceQrResponse;
import com.gighub.attendance.exception.WorkplaceQrIntegrityException;
import com.gighub.attendance.mapper.QrTokenMapper;
import com.gighub.attendance.mapper.result.QrTokenRow;
import com.gighub.attendance.qr.QrHmacKeys;
import com.gighub.attendance.qr.QrTokenCodec;
import com.gighub.attendance.service.impl.WorkplaceQrServiceImpl;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.member.domain.UserRole;
import com.gighub.workplace.mapper.WorkplaceMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** 고정 QR 조회 계약을 DB 없이 검증합니다. */
class WorkplaceQrServiceImplTest {

    private static final byte[] NONCE = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16
    };

    private final WorkplaceMapper workplaceMapper = mock(WorkplaceMapper.class);
    private final QrTokenMapper qrTokenMapper = mock(QrTokenMapper.class);
    private final QrTokenCodec codec = new QrTokenCodec(new QrHmacKeys("k1",
            Map.of("k1", "01234567890123456789012345678901".getBytes(StandardCharsets.US_ASCII))));
    private final WorkplaceQrServiceImpl service =
            new WorkplaceQrServiceImpl(workplaceMapper, qrTokenMapper, codec);

    @Test
    void returnsTokenSignedFromTheStoredNonce() {
        when(workplaceMapper.countOwnedActiveById(42L, 7L)).thenReturn(1);
        when(qrTokenMapper.findActiveByWorkplaceId(42L)).thenReturn(row());

        WorkplaceQrResponse response = service.findQr(owner(7L), 42L);

        assertEquals(42L, response.getWorkplaceId());
        assertEquals(codec.sign(42L, NONCE), response.getQrToken());
        assertTrue(codec.verify(response.getQrToken()).isPresent());
    }

    @Test
    void repeatedLookupsReturnTheSameToken() {
        when(workplaceMapper.countOwnedActiveById(42L, 7L)).thenReturn(1);
        when(qrTokenMapper.findActiveByWorkplaceId(42L)).thenReturn(row());

        assertEquals(
                service.findQr(owner(7L), 42L).getQrToken(),
                service.findQr(owner(7L), 42L).getQrToken());
    }

    @Test
    void rejectsNonOwnerBeforeTouchingStorage() {
        AuthPrincipal worker = new AuthPrincipal(9L, UserRole.WORKER, "김근로");

        assertThrows(RoleMismatchException.class, () -> service.findQr(worker, 42L));

        verifyNoInteractions(workplaceMapper);
        verifyNoInteractions(qrTokenMapper);
    }

    /** 남의 사업장과 없는 사업장을 구분하면 식별자의 존재가 드러납니다. 둘 다 404입니다. */
    @Test
    void reportsNotFoundForAWorkplaceTheCallerDoesNotOwn() {
        when(workplaceMapper.countOwnedActiveById(anyLong(), anyLong())).thenReturn(0);

        assertThrows(ResourceNotFoundException.class, () -> service.findQr(owner(7L), 42L));

        verifyNoInteractions(qrTokenMapper);
    }

    /** 활성 QR 없는 ACTIVE 사업장은 정상 상태가 아니므로 빈 응답으로 감추지 않습니다. */
    @Test
    void reportsIntegrityFailureWhenTheActiveWorkplaceHasNoQr() {
        when(workplaceMapper.countOwnedActiveById(42L, 7L)).thenReturn(1);
        when(qrTokenMapper.findActiveByWorkplaceId(42L)).thenReturn(null);

        assertThrows(WorkplaceQrIntegrityException.class, () -> service.findQr(owner(7L), 42L));
    }

    private static QrTokenRow row() {
        QrTokenRow row = new QrTokenRow();
        row.setId(1L);
        row.setWorkplaceId(42L);
        row.setTokenNonce(NONCE);
        row.setCreatedAt(LocalDateTime.of(2026, 8, 7, 9, 0));
        return row;
    }

    private static AuthPrincipal owner(long userId) {
        return new AuthPrincipal(userId, UserRole.OWNER, "김사장");
    }
}
