package com.gighub.bank.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankAccountPreflightCommand {
    Long accountId;
    Long userId;
}
