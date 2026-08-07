package com.gighub.wallet.service.command;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/** HTTP Query DTO와 분리한 지갑 거래 조회 조건입니다. */
@Getter
@Builder
public class WalletTransactionCriteria {

    private final Long workplaceId;
    private final LocalDate from;
    private final LocalDate to;
    private final String type;
    private final Long minAmount;
    private final Long maxAmount;
    private final String keyword;
    private final String sort;
    private final int page;
    private final int size;
}
