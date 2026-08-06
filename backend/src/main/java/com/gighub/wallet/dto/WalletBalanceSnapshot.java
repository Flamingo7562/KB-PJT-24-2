package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 no-args 생성자 없이 필드를 final로 고정한다. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class WalletBalanceSnapshot {
    private final Long walletId;
    private final Long userId;
    private final Long availableBalance;
    private final Long lockedBalance;
}
