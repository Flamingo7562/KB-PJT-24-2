package com.gighub.wallet.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Getter
@Builder
@Jacksonized
public class EscrowHoldRequest {
    @NotNull
    private Long employerId;  // 스파이크 입력값 (향후 토큰으로 대체)

    @NotNull
    private Long workerId;   // 스파이크 입력값 (향후 토큰으로 대체)

    @NotNull
    private Long workCaseId;

    @NotNull
    @Positive
    private Long amount;
}
