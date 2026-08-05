package com.gighub.wallet.dto;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.gighub.wallet.service.result.FundingResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 응답 필드가 승인된 3개로 고정되고, 계좌 ID·PIN 등 내부 정보가 섞이지 않는지 검증한다
 * (DEC-FUNDING-PIN, DEC-BANK-INPUT).
 */
class FundingOrderResponseTest {

    @Test
    void mapsResultFieldsOneToOne() {
        FundingResult result = FundingResult.builder()
                .fundingOrderId(21L)
                .status("COMPLETED")
                .bankTransactionId(31L)
                .availableBalance(1_000_000L)
                .lockedBalance(0L)
                .replayed(false)
                .build();

        FundingOrderResponse response = FundingOrderResponse.from(result);

        assertEquals(21L, response.getFundingOrderId());
        assertEquals("COMPLETED", response.getStatus());
        assertEquals(31L, response.getBankTransactionId());
    }

    @Test
    void exposesOnlyApprovedFields() {
        Set<String> fieldNames = Arrays.stream(FundingOrderResponse.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("fundingOrderId", "status", "bankTransactionId"), fieldNames);
    }
}
