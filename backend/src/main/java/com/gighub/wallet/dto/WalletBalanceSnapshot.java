package com.gighub.wallet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletBalanceSnapshot {
    private Long walletId;
    private Long userId;
    private Long availableBalance;
    private Long lockedBalance;
}
