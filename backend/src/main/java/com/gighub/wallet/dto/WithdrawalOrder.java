package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * withdrawal_requests 조회 결과.
 *
 * <p>MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 no-args 생성자 없이 필드를 final로 고정한다.</p>
 */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class WithdrawalOrder {
    private final Long id;
    private final Long userId;
    private final Long walletId;
    private final Long linkedAccountId;
    private final Long amount;
    private final Long mockBankTransactionId;
    private final String status;
}
