package com.gighub.wallet.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Setter
@NoArgsConstructor
public class WithdrawalRequest {
    @NotNull
    @Positive
    private Long bankAccountId;

    @NotNull
    @Positive
    private Long amount;
}
