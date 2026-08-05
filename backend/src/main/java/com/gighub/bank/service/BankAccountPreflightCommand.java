package com.gighub.bank.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankAccountPreflightCommand {
    Long accountId;
    // 충전 방향에서만 채운다. null이면 PIN을 검사하지 않는다(출금 입금 방향).
    String pin;
}
