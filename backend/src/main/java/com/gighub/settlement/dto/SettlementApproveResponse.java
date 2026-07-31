package com.gighub.settlement.dto;

import com.gighub.common.api.ApiTimes;
import com.gighub.settlement.service.result.SettlementResult;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SettlementApproveResponse {
    Long settlementId;
    String status;
    Instant completedAt;

    public static SettlementApproveResponse from(SettlementResult result) {
        return SettlementApproveResponse.builder()
                .settlementId(result.getSettlementId())
                .status(result.getStatus())
                .completedAt(ApiTimes.toInstant(result.getCompletedAt()))
                .build();
    }
}
