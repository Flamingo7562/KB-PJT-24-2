package com.gighub.wallet.service.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WithdrawalCommand {
    Long userId;
    Long linkedAccountId;
    Long amount;
    String idempotencyKey;
}
