package com.gighub.wallet.dto;

import com.gighub.wallet.service.result.FundingResult;
import lombok.Builder;
import lombok.Value;

/**
 * 지갑 충전 주문 응답입니다.
 *
 * <p>최신 잔액은 응답에 합치지 않습니다. 충전 성공 뒤 {@code GET /api/wallet}으로 다시
 * 조회합니다.</p>
 */
@Value
@Builder
public class FundingOrderResponse {

    Long fundingOrderId;
    String status;
    Long bankTransactionId;

    public static FundingOrderResponse from(FundingResult result) {
        return FundingOrderResponse.builder()
                .fundingOrderId(result.getFundingOrderId())
                .status(result.getStatus())
                .bankTransactionId(result.getBankTransactionId())
                .build();
    }
}
