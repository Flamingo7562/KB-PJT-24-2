package com.gighub.work.mapper.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 근무 상세의 에스크로 요약입니다. */
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
public class EscrowSummaryRow {

    private final String status;
    private final Long amount;
}
