package com.gighub.bank.mapper;

import com.gighub.bank.dto.MockBankAccount;
import com.gighub.bank.mapper.param.BankTransactionParam;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MockBankMapper {
    List<MockBankAccount> findActiveAccountsByUserId(@Param("userId") Long userId);

    MockBankAccount getAccountById(@Param("accountId") Long accountId);

    // 지갑 잠금 이후 호출
    MockBankAccount getAccountForUpdate(@Param("accountId") Long accountId);

    // 계좌 출금(충전 시)
    int withdrawFromAccount(@Param("accountId") Long accountId, @Param("amount") Long amount);

    // 계좌 입금(출금 시)
    int depositToAccount(@Param("accountId") Long accountId, @Param("amount") Long amount);

    int insertBankTransaction(BankTransactionParam param);
}
