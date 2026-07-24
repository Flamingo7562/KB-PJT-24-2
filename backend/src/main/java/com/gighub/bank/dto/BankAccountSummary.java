package com.gighub.bank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BankAccountSummary {
    private Long bankAccountId;
    private String bankCode;
    private String maskedAccountNumber;
    private String currency;
    private Long balance;
    private Long availableAmount;
    private String status;
}
