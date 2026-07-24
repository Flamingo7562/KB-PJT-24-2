package com.gighub.bank.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankTransferResult {
    Long bankTransactionId;
    String bankTranId;
    Long balanceBefore;
    Long balanceAfter;
}
