package com.gighub.work.mapper.result;

import com.gighub.settlement.domain.SettlementStatus;
import com.gighub.work.domain.WorkCaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/** WORKER 홈과 이력 조회가 공유하는 저장 상태 Snapshot입니다. */
@Getter
@Builder
@AllArgsConstructor
public class WorkerWorkCaseRow {

    private final Long workCaseId;
    private final String title;
    private final String workplaceName;
    private final LocalDateTime startsAt;
    private final LocalDateTime endsAt;
    private final Integer breakMinutes;
    private final Boolean breakPaid;
    private final Long dailyWage;
    private final WorkCaseStatus status;
    private final LocalDateTime checkedInAt;
    private final LocalDateTime checkedOutAt;
    private final String escrowStatus;
    private final SettlementStatus settlementStatus;
    private final LocalDateTime settlementDueAt;
}
