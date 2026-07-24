package com.gighub.wallet.service.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FundingCommand {
    Long employerId;
    Long linkedAccountId;
    Long amount;
    String idempotencyKey;
}
