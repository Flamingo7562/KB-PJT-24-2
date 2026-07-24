package com.gighub.settlement.dto;

import com.gighub.settlement.domain.SettlementStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class SettlementSnapshot {
    private Long settlementId;
    private Long workCaseId;
    private Long amount;
    private SettlementStatus status;
    private Long approvedByUserId;
    private LocalDateTime dueAt;
    private LocalDateTime processingAt;
    private LocalDateTime completedAt;
    private String failureCode;
}
