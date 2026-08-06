package com.gighub.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MockBankAccount {
    private Long id;
    private String bankCode;
    private String mockAccountNumber;
    private String pin;
    private Long balance;
    private Long availableAmount;
    private String status;
}
