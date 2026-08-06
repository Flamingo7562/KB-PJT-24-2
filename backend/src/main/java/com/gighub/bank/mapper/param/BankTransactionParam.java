package com.gighub.bank.mapper.param;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class BankTransactionParam {
    // useGeneratedKeys가 insert 이후 이 필드에 직접 채우므로 setter를 유지한다.
    @Setter
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
