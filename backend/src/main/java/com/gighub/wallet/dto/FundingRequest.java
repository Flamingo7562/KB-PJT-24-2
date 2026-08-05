package com.gighub.wallet.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Builder
@Jacksonized
public class FundingRequest {
    @NotNull
    @Positive
    private Long bankAccountId;

    @NotNull
    @Positive
    private Long amount;
}
