package com.gighub.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** MyBatis가 &lt;constructor&gt; 매핑으로 생성하므로 no-args 생성자 없이 필드를 final로 고정한다. */
@Getter
@Builder
@AllArgsConstructor
public class MockBankAccount {
    private final Long id;
    private final String bankCode;
    private final String mockAccountNumber;
    private final String pin;
    private final Long balance;
    private final Long availableAmount;
    private final String status;
}
