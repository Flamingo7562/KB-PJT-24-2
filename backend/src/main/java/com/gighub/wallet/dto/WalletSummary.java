package com.gighub.wallet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletSummary {
    private Long walletId;
    private Long availableBalance;
    private Long lockedBalance;
}
