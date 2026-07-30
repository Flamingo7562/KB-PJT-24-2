package com.gighub.bank.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class BankTransactionParam {
    private Long id;
    private Long accountId;
    private String bankTranId;
    private String transferType; // WITHDRAW | DEPOSIT
    private Long amount;
    private Long balanceBefore;
    private Long balanceAfter;
    private String referenceType; // FUNDING_ORDER | WITHDRAWAL_REQUEST
    private Long referenceId;
}
