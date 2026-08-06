package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// withdrawal_requests 조회 결과
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalOrder {
    private Long id;
    private Long userId;
    private Long walletId;
    private Long linkedAccountId;
    private Long amount;
    private Long mockBankTransactionId;
    private String status;
}
