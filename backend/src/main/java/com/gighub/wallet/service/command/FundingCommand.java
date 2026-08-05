package com.gighub.wallet.service.command;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FundingCommand {
    Long employerId;
    String bankCode;
    String accountNo;
    String pin;
    Long amount;
    String idempotencyKey;
}
