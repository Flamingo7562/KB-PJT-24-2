package com.gighub.bank.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockBankAccount {
    private Long id;
    private Long userId;
    private String bankCode;
    private String mockAccountNumber;
    private Long balance;
    private Long availableAmount;
    private String status;
}
