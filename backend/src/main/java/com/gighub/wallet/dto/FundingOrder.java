package com.gighub.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class FundingOrder { // 조회용
    private Long id;
    private Long employerId;
    private Long linkedAccountId;
    private Long expectedAmount;
    private Long transferredAmount;
    private Long mockBankTransactionId;
    private String status;
}
