package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionSnapshot {
    private Long id;
    private Long walletId;
    private Long walletUserId;
    private Long workCaseId;
    private String transactionType;
    private Long amount;
    private Long availableBefore;
    private Long availableAfter;
    private Long lockedBefore;
    private Long lockedAfter;
    private String referenceType;
    private Long referenceId;
}
