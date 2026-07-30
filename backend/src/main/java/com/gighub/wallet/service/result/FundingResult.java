package com.gighub.wallet.service.result;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FundingResult {
    Long fundingOrderId;
    String status;
    Long bankTransactionId;
    Long availableBalance;
    Long lockedBalance;
    // true면 컨트롤러가 200 + Idempotency-Replayed 헤더로 응답한다.
    boolean replayed;
}
