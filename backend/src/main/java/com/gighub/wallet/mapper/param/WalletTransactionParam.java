package com.gighub.wallet.mapper.param;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WalletTransactionParam {
    Long walletId;
    Long workCaseId;
    String transactionType;
    Long amount;
    Long availableBefore;
    Long availableAfter;
    Long lockedBefore;
    Long lockedAfter;
    String referenceType;
    Long referenceId;
    String idempotencyKey;
}
