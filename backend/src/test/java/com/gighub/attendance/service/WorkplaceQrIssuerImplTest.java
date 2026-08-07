package com.gighub.attendance.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gighub.attendance.mapper.QrTokenMapper;
import com.gighub.attendance.mapper.param.QrTokenInsertParam;
import com.gighub.attendance.qr.QrTokenCodec;
import com.gighub.attendance.service.impl.WorkplaceQrIssuerImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 활성 고정 QR 발급 계약을 DB 없이 검증합니다. */
class WorkplaceQrIssuerImplTest {

    private final QrTokenMapper qrTokenMapper = mock(QrTokenMapper.class);
    private final WorkplaceQrIssuerImpl issuer = new WorkplaceQrIssuerImpl(qrTokenMapper);

    @Test
    void insertsOneActiveQrOwnedByTheWorkplaceOwner() {
        when(qrTokenMapper.insertActive(any(QrTokenInsertParam.class))).thenReturn(1);

        byte[] issued = issuer.issueActive(42L, 7L);

        ArgumentCaptor<QrTokenInsertParam> captor =
                ArgumentCaptor.forClass(QrTokenInsertParam.class);
        verify(qrTokenMapper).insertActive(captor.capture());

        QrTokenInsertParam param = captor.getValue();
        assertEquals(42L, param.getWorkplaceId());
        assertEquals(7L, param.getIssuedByUserId());
        assertEquals(QrTokenCodec.NONCE_LENGTH, param.getTokenNonce().length);
        // 호출자는 이 값으로 Token을 서명합니다. 저장한 것과 다르면 사장이 인쇄한 QR과
        // DB의 활성 QR이 영구히 어긋납니다.
        assertArrayEquals(param.getTokenNonce(), issued);
    }

    @Test
    void generatesADistinctNonceEveryTime() {
        when(qrTokenMapper.insertActive(any(QrTokenInsertParam.class))).thenReturn(1);

        for (int attempt = 0; attempt < 50; attempt++) {
            issuer.issueActive(42L, 7L);
        }

        ArgumentCaptor<QrTokenInsertParam> captor =
                ArgumentCaptor.forClass(QrTokenInsertParam.class);
        verify(qrTokenMapper, times(50)).insertActive(captor.capture());

        Set<String> seen = new HashSet<>();
        captor.getAllValues().forEach(param -> seen.add(Arrays.toString(param.getTokenNonce())));

        // token_nonce는 전역 Unique Column이므로 재사용되면 다음 발급이 실패합니다.
        assertEquals(50, seen.size());
    }

    @Test
    void reportsIntegrityFailureWhenNoRowIsInserted() {
        when(qrTokenMapper.insertActive(any(QrTokenInsertParam.class))).thenReturn(0);

        IllegalStateException thrown =
                assertThrows(IllegalStateException.class, () -> issuer.issueActive(42L, 7L));

        assertTrue(thrown.getMessage().contains("42"));
    }
}
