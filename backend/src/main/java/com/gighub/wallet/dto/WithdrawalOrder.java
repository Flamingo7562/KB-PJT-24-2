package com.gighub.wallet.dto;

import lombok.Getter;
import lombok.Setter;

// withdrawal_requests 조회 결과
@Getter
@Setter
public class WithdrawalOrder {
    private Long id;
    private Long userId;
    private Long walletId;
    private Long linkedAccountId;
    private Long amount;
    private Long mockBankTransactionId;
    private String status;
}
