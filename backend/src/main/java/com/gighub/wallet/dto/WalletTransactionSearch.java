package com.gighub.wallet.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 지갑 거래 내역 검색 조건
@Getter
@Builder
public class WalletTransactionSearch {
    private final Long userId;
    private final Long workplaceId;
    private final LocalDateTime from;
    private final LocalDateTime toExclusive;
    private final String type;
    private final Long minAmount;
    private final Long maxAmount;
    private final String keyword;
    private final String sort;
    private final int offset;
    private final int size;
}
