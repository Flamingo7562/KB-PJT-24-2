package com.gighub.bank.service.impl;

import com.gighub.bank.dto.MockBankAccount;
import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.exception.BankTransferIntegrityException;
import com.gighub.bank.exception.InsufficientBankBalanceException;
import com.gighub.bank.mapper.MockBankMapper;
import com.gighub.bank.mapper.param.BankTransactionParam;
import com.gighub.bank.service.BankAccountPreflightCommand;
import com.gighub.bank.service.BankTransferCommand;
import com.gighub.bank.service.BankTransferGateway;
import com.gighub.bank.service.BankTransferResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MockBankTransferGateway implements BankTransferGateway {

    private static final String ACCOUNT_ACTIVE = "ACTIVE";
    private static final String TRANSFER_WITHDRAW = "WITHDRAW";
    private static final String TRANSFER_DEPOSIT = "DEPOSIT";
    private static final String TRANSFER_SUCCESS = "SUCCESS";
    // 계좌 미존재·비활성·PIN 불일치를 구분 없이 같은 승인 오류로 응답한다(DEC-BANK-ERROR-CATALOG).
    private static final String ACCOUNT_UNUSABLE_MESSAGE = "계좌를 사용할 수 없습니다.";

    private final MockBankMapper mockBankMapper;

    @Override
    public Long resolveAccountId(String bankCode, String accountNo) {
        if (bankCode == null || accountNo == null) {
            throw new BankAccountForbiddenException(ACCOUNT_UNUSABLE_MESSAGE);
        }
        Long accountId = mockBankMapper.findAccountIdByBankCodeAndAccountNo(bankCode, accountNo);
        if (accountId == null) {
            throw new BankAccountForbiddenException(ACCOUNT_UNUSABLE_MESSAGE);
        }
        return accountId;
    }

    @Override
    public void lockAccount(Long accountId) {
        if (accountId == null || accountId <= 0) {
            throw new BankTransferIntegrityException("은행 계좌 잠금 요청이 올바르지 않습니다.");
        }
        // 존재만 확인한다. 상태·PIN 검증은 claim 선점 이후 preflight의 책임이다.
        if (mockBankMapper.getAccountForUpdate(accountId) == null) {
            throw new BankAccountForbiddenException(ACCOUNT_UNUSABLE_MESSAGE);
        }
    }

    @Override
    public void preflight(BankAccountPreflightCommand command) {
        validatePreflightCommand(command);
        validate(
                mockBankMapper.getAccountById(command.getAccountId()),
                command.getAccountId(),
                command.getPin()
        );
    }

    @Override
    public BankTransferResult withdraw(BankTransferCommand command) {
        MockBankAccount account = lockAndValidate(command);

        validateBalances(account);
        if (account.getAvailableAmount() < command.getAmount()
                || account.getBalance() < command.getAmount()) {
            throw new InsufficientBankBalanceException("연결 계좌의 가용 잔액이 부족합니다.");
        }
        Long balanceAfter = subtractExactly(account.getBalance(), command.getAmount());
        if (mockBankMapper.withdrawFromAccount(command.getAccountId(), command.getAmount()) != 1) {
            throw new BankTransferIntegrityException("잠금 계좌 출금 결과가 예상과 다릅니다.");
        }
        return record(command, TRANSFER_WITHDRAW,
                account.getBalance(), balanceAfter);
    }

    @Override
    public BankTransferResult deposit(BankTransferCommand command) {
        MockBankAccount account = lockAndValidate(command);

        validateBalances(account);
        Long balanceAfter = addExactly(account.getBalance(), command.getAmount());
        if (mockBankMapper.depositToAccount(command.getAccountId(), command.getAmount()) != 1) {
            throw new BankTransferIntegrityException("잠금 계좌 입금 결과가 예상과 다릅니다.");
        }
        return record(command, TRANSFER_DEPOSIT,
                account.getBalance(), balanceAfter);
    }

    // lockAccount()가 같은 트랜잭션에서 이미 X-lock을 잡았으므로(BankTransferGateway 계약),
    // 여기서는 FOR UPDATE를 반복하지 않고 비잠금 조회로 같은 잠긴 행을 읽는다.
    private MockBankAccount lockAndValidate(BankTransferCommand command) {
        validateTransferCommand(command);
        return validate(
                mockBankMapper.getAccountById(command.getAccountId()),
                command.getAccountId(),
                command.getPin()
        );
    }

    // pin이 null이면 출금(입금) 방향이라 PIN을 검사하지 않는다.
    private MockBankAccount validate(MockBankAccount account, Long accountId, String expectedPin) {
        if (account == null || !Objects.equals(account.getId(), accountId)) {
            throw new BankAccountForbiddenException(ACCOUNT_UNUSABLE_MESSAGE);
        }
        if (account.getStatus() == null || !ACCOUNT_ACTIVE.equals(account.getStatus())) {
            throw new BankAccountForbiddenException(ACCOUNT_UNUSABLE_MESSAGE);
        }
        if (expectedPin != null && !expectedPin.equals(account.getPin())) {
            throw new BankAccountForbiddenException(ACCOUNT_UNUSABLE_MESSAGE);
        }
        return account;
    }

    private void validatePreflightCommand(BankAccountPreflightCommand command) {
        if (command == null
                || command.getAccountId() == null
                || command.getAccountId() <= 0) {
            throw new BankTransferIntegrityException("은행 계좌 사전 검증 요청이 올바르지 않습니다.");
        }
    }

    private void validateTransferCommand(BankTransferCommand command) {
        if (command == null
                || command.getAccountId() == null
                || command.getAccountId() <= 0
                || command.getAmount() == null
                || command.getAmount() <= 0
                || command.getReferenceType() == null
                || command.getReferenceType().isBlank()
                || command.getReferenceType().length() > 30
                || command.getReferenceId() == null
                || command.getReferenceId() <= 0) {
            throw new BankTransferIntegrityException("은행 이체 요청 계약이 올바르지 않습니다.");
        }
    }

    private void validateBalances(MockBankAccount account) {
        if (account.getBalance() == null
                || account.getAvailableAmount() == null
                || account.getBalance() < 0
                || account.getAvailableAmount() < 0
                || account.getAvailableAmount() > account.getBalance()) {
            throw new BankTransferIntegrityException("조회된 은행 계좌 잔액이 올바르지 않습니다.");
        }
    }

    private Long addExactly(Long left, Long right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            throw new BankTransferIntegrityException("은행 입금 후 잔액이 허용 범위를 벗어났습니다.");
        }
    }

    private Long subtractExactly(Long left, Long right) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException overflow) {
            throw new BankTransferIntegrityException("은행 출금 후 잔액이 허용 범위를 벗어났습니다.");
        }
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
        if (mockBankMapper.insertBankTransaction(param) != 1
                || param.getId() == null || param.getId() <= 0) {
            throw new BankTransferIntegrityException(
                    "은행 거래 원장이 정상적으로 기록되지 않았습니다."
            );
        }

        return BankTransferResult.builder()
                .bankTransactionId(param.getId())
                .bankTranId(param.getBankTranId())
                .status(TRANSFER_SUCCESS)
                .transferredAmount(command.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .build();
    }

    /** bank_tran_id는 ascii varchar(64) UNIQUE. 실제 은행이 발급하는 값을 흉내낸다. */
    private String generateBankTranId() {
        return "M" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }

}
