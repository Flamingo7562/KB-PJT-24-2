package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 no-args 생성자 없이 필드를 final로 고정한다. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class WalletTransactionSnapshot {
    private final Long id;
    private final Long walletId;
    private final Long walletUserId;
    private final Long workCaseId;
    private final String transactionType;
    private final Long amount;
    private final Long availableBefore;
    private final Long availableAfter;
    private final Long lockedBefore;
    private final Long lockedAfter;
    private final String referenceType;
    private final Long referenceId;
}
