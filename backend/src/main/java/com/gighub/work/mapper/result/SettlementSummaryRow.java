package com.gighub.work.mapper.result;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 근무 상세의 정산 요약입니다. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class SettlementSummaryRow {

    private final String status;
    private final Long amount;
    private final LocalDateTime dueAt;
    private final LocalDateTime completedAt;
}
