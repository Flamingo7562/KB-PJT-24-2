package com.gighub.bank.mapper;

import com.gighub.bank.dto.MockBankAccount;
import com.gighub.bank.mapper.param.BankTransactionParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MockBankMapper {
    // 비귀속 Fixture 식별. status와 무관하게 존재 자체만 조회한다(Idempotency Replay가
    // 현재 상태와 무관하게 원래 계좌를 다시 찾을 수 있어야 한다).
    Long findAccountIdByBankCodeAndAccountNo(
            @Param("bankCode") String bankCode, @Param("accountNo") String accountNo);

    MockBankAccount getAccountById(@Param("accountId") Long accountId);

    // 지갑 잠금 이후 호출
    MockBankAccount getAccountForUpdate(@Param("accountId") Long accountId);

    // 계좌 출금(충전 시)
    int withdrawFromAccount(@Param("accountId") Long accountId, @Param("amount") Long amount);

    // 계좌 입금(출금 시)
    int depositToAccount(@Param("accountId") Long accountId, @Param("amount") Long amount);

    int insertBankTransaction(BankTransactionParam param);
}
