package com.gighub.wallet.dto;

import com.gighub.wallet.service.result.WithdrawalResult;
import lombok.Builder;
import lombok.Value;

/**
 * 지갑 출금 요청 응답입니다.
 *
 * <p>최신 잔액은 응답에 합치지 않습니다. 출금 성공 뒤 {@code GET /api/wallet}으로 다시
 * 조회합니다.</p>
 */
@Value
@Builder
public class WithdrawalRequestResponse {

    Long withdrawalRequestId;
    String status;
    Long bankTransactionId;

    public static WithdrawalRequestResponse from(WithdrawalResult result) {
        return WithdrawalRequestResponse.builder()
                .withdrawalRequestId(result.getWithdrawalRequestId())
                .status(result.getStatus())
                .bankTransactionId(result.getBankTransactionId())
                .build();
    }
}
