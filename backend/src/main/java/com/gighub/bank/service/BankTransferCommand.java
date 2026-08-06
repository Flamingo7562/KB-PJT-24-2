package com.gighub.bank.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankTransferCommand {
    Long accountId;
    // 충전(withdraw 방향)에서만 채운다. null이면 PIN을 검사하지 않는다(출금 deposit 방향).
    String pin;
    Long amount;
    String referenceType;
    Long referenceId;
}
