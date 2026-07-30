package com.gighub.wallet.service.result;

import lombok.Builder;
import lombok.Value;

// 확정된 계약에 따라 잔액은 포함되지 않음, 최신 잔액은 GET /api/wallet으로 조회
@Value
@Builder
public class WithdrawalResult {
    Long withdrawalRequestId;
    String status;
    Long bankTransactionId;
    // 응답 본문이 아닌 Idempotency-Replayed 헤더 결정에만 사용한다.
    boolean replayed;
}
