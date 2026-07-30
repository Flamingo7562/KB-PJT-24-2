package com.gighub.bank.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankTransferCommand {
    Long accountId;
    Long userId; // 소유권 재검증
    Long amount;
    String referenceType;
    Long referenceId;
}
