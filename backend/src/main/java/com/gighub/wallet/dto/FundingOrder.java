package com.gighub.wallet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FundingOrder { // 조회용
    private Long id;
    private Long employerId;
    private Long linkedAccountId;
    private Long expectedAmount;
    private Long transferredAmount;
    private Long mockBankTransactionId;
    private String status;
}
