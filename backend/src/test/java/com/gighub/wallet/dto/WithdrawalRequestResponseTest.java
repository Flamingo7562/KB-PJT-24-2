package com.gighub.wallet.dto;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.gighub.wallet.service.result.WithdrawalResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 응답 필드가 승인된 3개로 고정되고, 계좌 ID·잔액 등 내부 정보가 섞이지 않는지 검증한다
 * (DEC-WITHDRAWAL-DESTINATION, DEC-BANK-INPUT).
 */
class WithdrawalRequestResponseTest {

    @Test
    void mapsResultFieldsOneToOne() {
        WithdrawalResult result = WithdrawalResult.builder()
                .withdrawalRequestId(22L)
                .status("COMPLETED")
                .bankTransactionId(33L)
                .replayed(false)
                .build();

        WithdrawalRequestResponse response = WithdrawalRequestResponse.from(result);

        assertEquals(22L, response.getWithdrawalRequestId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(33L, response.getBankTransactionId());
    }

    @Test
    void exposesOnlyApprovedFields() {
        Set<String> fieldNames =
                Arrays.stream(WithdrawalRequestResponse.class.getDeclaredFields())
                        .map(Field::getName)
                        .collect(Collectors.toSet());

        assertEquals(Set.of("withdrawalRequestId", "status", "bankTransactionId"), fieldNames);
    }
}
