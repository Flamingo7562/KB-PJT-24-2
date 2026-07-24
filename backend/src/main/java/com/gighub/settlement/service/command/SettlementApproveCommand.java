package com.gighub.settlement.service.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SettlementApproveCommand {
    Long workCaseId;
    Long approverUserId;
    String idempotencyKey;
}
