package com.gighub.settlement.dto;

import com.gighub.settlement.service.result.SettlementResult;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class SettlementApproveResponse {
    Long settlementId;
    String status;
    LocalDateTime completedAt;

    public static SettlementApproveResponse from(SettlementResult result) {
        return SettlementApproveResponse.builder()
                .settlementId(result.getSettlementId())
                .status(result.getStatus())
                .completedAt(result.getCompletedAt())
                .build();
    }
}
