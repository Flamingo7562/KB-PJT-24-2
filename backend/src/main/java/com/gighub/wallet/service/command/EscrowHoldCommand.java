package com.gighub.wallet.service.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class EscrowHoldCommand {
    Long employerId;
    Long workerId;
    Long workCaseId;
    Long amount;
    String idempotencyKey;
}
