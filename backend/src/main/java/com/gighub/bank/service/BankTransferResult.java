package com.gighub.bank.service;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BankTransferResult {
    Long bankTransactionId;
    String bankTranId;
    String status;
    Long transferredAmount;
    Long balanceBefore;
    Long balanceAfter;
}
