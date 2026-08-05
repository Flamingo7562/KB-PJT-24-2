package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WalletBalanceSnapshot {
    private Long walletId;
    private Long userId;
    private Long availableBalance;
    private Long lockedBalance;
}
