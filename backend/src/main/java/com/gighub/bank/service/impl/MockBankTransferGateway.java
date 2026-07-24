package com.gighub.bank.service.impl;

import com.gighub.bank.dto.MockBankAccount;
import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.exception.BankTransferIntegrityException;
import com.gighub.bank.exception.InsufficientBankBalanceException;
import com.gighub.bank.exception.InvalidBankAccountStateException;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class MockBankTransferGateway implements BankTransferGateway {

    private static final String ACCOUNT_ACTIVE = "ACTIVE";
    private static final String TRANSFER_WITHDRAW = "WITHDRAW";
    private static final String TRANSFER_DEPOSIT = "DEPOSIT";
    private static final String TRANSFER_SUCCESS = "SUCCESS";
    private static final Set<String> ACCOUNT_STATUSES =
            Set.of(ACCOUNT_ACTIVE, "BLOCKED", "CLOSED");

    private final MockBankMapper mockBankMapper;

    @Override
    public void preflight(BankAccountPreflightCommand command) {
        validatePreflightCommand(command);
        validate(
                mockBankMapper.getAccountById(command.getAccountId()),
                command.getAccountId(),
                command.getUserId()
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

    private MockBankAccount lockAndValidate(BankTransferCommand command) {
        validateTransferCommand(command);
        return validate(
                mockBankMapper.getAccountForUpdate(command.getAccountId()),
                command.getAccountId(),
                command.getUserId()
        );
    }

    private MockBankAccount validate(MockBankAccount account, Long accountId, Long userId) {
        if (account == null) {
            throw new BankAccountForbiddenException("연결 계좌를 찾을 수 없습니다.");
        }
        if (!Objects.equals(account.getId(), accountId)
                || account.getUserId() == null) {
            throw new BankTransferIntegrityException("조회된 은행 계좌 식별자가 올바르지 않습니다.");
        }
        if (!Objects.equals(account.getUserId(), userId)) {
            throw new BankAccountForbiddenException("본인 소유 계좌가 아닙니다.");
        }
        if (account.getStatus() == null
                || !ACCOUNT_STATUSES.contains(account.getStatus())) {
            throw new BankTransferIntegrityException("조회된 은행 계좌 상태가 올바르지 않습니다.");
        }
        if (!ACCOUNT_ACTIVE.equals(account.getStatus())) {
            throw new InvalidBankAccountStateException("사용할 수 없는 계좌 상태입니다.");
        }
        return account;
    }

    private void validatePreflightCommand(BankAccountPreflightCommand command) {
        if (command == null
                || command.getAccountId() == null
                || command.getAccountId() <= 0
                || command.getUserId() == null
                || command.getUserId() <= 0) {
            throw new BankTransferIntegrityException("은행 계좌 사전 검증 요청이 올바르지 않습니다.");
        }
    }

    private void validateTransferCommand(BankTransferCommand command) {
        if (command == null
                || command.getAccountId() == null
                || command.getAccountId() <= 0
                || command.getUserId() == null
                || command.getUserId() <= 0
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
