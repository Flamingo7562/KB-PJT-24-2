package com.gighub.bank.service.impl;

import com.gighub.bank.dto.MockBankAccount;
import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.exception.InsufficientBankBalanceException;
import com.gighub.bank.exception.InvalidBankAccountStateException;
import com.gighub.bank.mapper.MockBankMapper;
import com.gighub.bank.mapper.param.BankTransactionParam;
import com.gighub.bank.service.BankTransferCommand;
import com.gighub.bank.service.BankTransferGateway;
import com.gighub.bank.service.BankTransferResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MockBankTransferGateway implements BankTransferGateway {

    private static final String ACCOUNT_ACTIVE="ACTIVE";
    private static String TRANSFER_WITHDRAW = "WITHDRAW";
    private static String TRANSFER_DEPOSIT = "DEPOSIT";

    private final MockBankMapper mockBankMapper;

    @Override
    public BankTransferResult withdraw(BankTransferCommand command) {
        MockBankAccount account = lockAndValidate(command);

        Long before = account.getAvailableAmount();
        if(before < command.getAmount()){
            throw new InsufficientBankBalanceException("연결 계좌의 가용 잔액이 부족합니다.");
        }
        if (mockBankMapper.withdrawFromAccount(command.getAccountId(), command.getAmount()) != 1) {
            throw new InsufficientBankBalanceException("연결 계좌의 가용 잔액이 부족합니다.");
        }
        return record(command, TRANSFER_WITHDRAW,
                account.getBalance(), account.getBalance() - command.getAmount());
    }

    @Override
    public BankTransferResult deposit(BankTransferCommand command) {
        MockBankAccount account = lockAndValidate(command);

        if (mockBankMapper.depositToAccount(command.getAccountId(), command.getAmount()) != 1) {
            throw new InvalidBankAccountStateException("연결 계좌에 입금할 수 없습니다.");
        }
        return record(command, TRANSFER_DEPOSIT,
                account.getBalance(), account.getBalance() + command.getAmount());
    }

    private MockBankAccount lockAndValidate(BankTransferCommand command) {
        MockBankAccount account = mockBankMapper.getAccountForUpdate(command.getAccountId());
        if (account == null) {
            throw new BankAccountForbiddenException("연결 계좌를 찾을 수 없습니다.");
        }
        if (!account.getUserId().equals(command.getUserId())) {
            throw new BankAccountForbiddenException("본인 소유 계좌가 아닙니다.");
        }
        if (!ACCOUNT_ACTIVE.equals(account.getStatus())) {
            throw new InvalidBankAccountStateException("사용할 수 없는 계좌 상태입니다.");
        }
        return account;
    }

    private BankTransferResult record(BankTransferCommand command, String transferType,
                                      Long balanceBefore, Long balanceAfter) {
        // uk_mock_bank_transactions_reference가 주문당 한 방향 1건을 보장한다.
        BankTransactionParam param = BankTransactionParam.builder()
                .accountId(command.getAccountId())
                .bankTranId(generateBankTranId())
                .transferType(transferType)
                .amount(command.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .referenceType(command.getReferenceType())
                .referenceId(command.getReferenceId())
                .build();
        mockBankMapper.insertBankTransaction(param);

        return BankTransferResult.builder()
                .bankTransactionId(param.getId())
                .bankTranId(param.getBankTranId())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
    }

    /** bank_tran_id는 ascii varchar(64) UNIQUE. 실제 은행이 발급하는 값을 흉내낸다. */
    private String generateBankTranId() {
        return "M" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

}
