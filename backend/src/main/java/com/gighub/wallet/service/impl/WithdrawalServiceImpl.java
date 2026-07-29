package com.gighub.wallet.service.impl;

import com.gighub.bank.exception.BankTransferIntegrityException;
import com.gighub.bank.service.BankAccountPreflightCommand;
import com.gighub.bank.service.BankTransferCommand;
import com.gighub.bank.service.BankTransferGateway;
import com.gighub.bank.service.BankTransferResult;
import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.dto.WalletTransactionSnapshot;
import com.gighub.wallet.dto.WithdrawalOrder;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InsufficientAvailableBalanceException;
import com.gighub.wallet.exception.InvalidWalletStateException;
import com.gighub.wallet.exception.InvalidWithdrawalRequestException;
import com.gighub.wallet.exception.WithdrawalIntegrityException;
import com.gighub.wallet.exception.WithdrawalIntegrityException;
import com.gighub.wallet.idempotency.WalletIdempotencyKeys;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.WithdrawalMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.wallet.mapper.param.WithdrawalOrderParam;
import com.gighub.wallet.service.WithdrawalService;
import com.gighub.wallet.service.command.WithdrawalCommand;
import com.gighub.wallet.service.result.WithdrawalResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class WithdrawalServiceImpl implements WithdrawalService {
    private static final int MAX_TRANSACTION_ATTEMPTS = 3;
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String TRANSFER_SUCCESS = "SUCCESS";
    private static final String TX_WITHDRAWAL = "WITHDRAWAL";
    private static final String REF_WITHDRAWAL_REQUEST = "WITHDRAWAL_REQUEST";

    private final WithdrawalMapper withdrawalMapper;
    private final WalletMapper walletMapper;
    private final BankTransferGateway bankTransferGateway;
    private final WithdrawalTransactionExecutor transactionExecutor;

    @Override
    public WithdrawalResult withdraw(WithdrawalCommand command) {
        validateCommand(command);
        String rawKey = WalletIdempotencyKeys.validateRawKey(command.getIdempotencyKey());
        String ledgerKey = WalletIdempotencyKeys.withdrawal(rawKey);

        int attemptCount = 0;
        while(true){
            attemptCount++;
            try{
                return transactionExecutor.execute(
                        () -> withdrawOnce(command, rawKey, ledgerKey)
                );
            }catch (PessimisticLockingFailureException retryable){
                if(attemptCount >= MAX_TRANSACTION_ATTEMPTS){
                    throw retryable;
                }
            }
        }
    }

    private WithdrawalResult withdrawOnce(
            WithdrawalCommand command, String rawKey, String ledgerKey) {
        // wallet_id는 NOT NULL FK이므로 선점 INSERT 전에 필요
        // 불변 식별자만 조회하고 잔액은 아래 잠금 스냅샷에서만 읽는다.
        Long walletId = walletMapper.getWalletIdByUserId(command.getUserId());
        if (walletId == null || walletId <= 0) {
            throw new InvalidWalletStateException("지갑을 찾을 수 없습니다.");
        }

        WithdrawalOrderParam order = WithdrawalOrderParam.builder()
                .userId(command.getUserId())
                .walletId(walletId)
                .linkedAccountId(command.getLinkedAccountId())
                .amount(command.getAmount())
                .idempotencyKey(rawKey)
                .build();

        try{
            if(withdrawalMapper.insertWithdrawalRequest(order) != 1 || order.getId() == null || order.getId() <= 0){
                throw new WithdrawalIntegrityException("출금 요청을 선점하지 못했습니다.");
            }
        }catch (DuplicateKeyException duplicate){
            return replayClaimedRequest(command, rawKey, ledgerKey, duplicate);
        }catch (DataIntegrityViolationException invalidOrder){
            translateInvalidOrderReference(command, invalidOrder);
        }

        bankTransferGateway.preflight(BankAccountPreflightCommand.builder()
                .accountId(command.getLinkedAccountId())
                .userId(command.getUserId())
                .build());

        // 전역 잠금 순서: 지갑 -> 계좌
        WalletBalanceSnapshot wallet =
                walletMapper.getWalletSnapshotForUpdate(command.getUserId());
        if (wallet == null) {
            throw new InvalidWalletStateException("지갑을 찾을 수 없습니다.");
        }
        validateWalletSnapshot(wallet, command.getUserId(), walletId);

        // 잠금 잔액은 출금할 수 없다
        if (wallet.getAvailableBalance() < command.getAmount()) {
            throw new InsufficientAvailableBalanceException("지갑의 가용 잔액이 부족합니다.");
        }
        Long availableAfter = subtractExactly(
                wallet.getAvailableBalance(),
                command.getAmount(),
                "지갑 출금 후 잔액이 허용 범위를 벗어났습니다."
        );

        BankTransferResult transfer = bankTransferGateway.deposit(BankTransferCommand.builder()
                .accountId(command.getLinkedAccountId())
                .userId(command.getUserId())
                .amount(command.getAmount())
                .referenceType(REF_WITHDRAWAL_REQUEST)
                .referenceId(order.getId())
                .build());
        validateTransferResult(transfer, command.getAmount());

        if (withdrawalMapper.completeWithdrawalRequest(
                order.getId(), transfer.getBankTransactionId()) != 1) {
            throw new WithdrawalIntegrityException("출금 요청 완료 상태를 기록하지 못했습니다.");
        }

        if (walletMapper.subtractAvailableBalance(
                command.getUserId(), command.getAmount()) != 1) {
            throw new WithdrawalIntegrityException("지갑 출금 잔액을 반영하지 못했습니다.");
        }

        // 출금: available만 감소, locked 불변, work_case_id는 NULL
        WalletTransactionParam transaction = WalletTransactionParam.builder()
                .walletId(wallet.getWalletId())
                .workCaseId(null)
                .transactionType(TX_WITHDRAWAL)
                .amount(command.getAmount())
                .availableBefore(wallet.getAvailableBalance())
                .availableAfter(availableAfter)
                .lockedBefore(wallet.getLockedBalance())
                .lockedAfter(wallet.getLockedBalance())
                .referenceType(REF_WITHDRAWAL_REQUEST)
                .referenceId(order.getId())
                .idempotencyKey(ledgerKey)
                .build();

        if (walletMapper.insertWalletTransaction(transaction) != 1) {
            throw new WithdrawalIntegrityException("지갑 출금 원장을 기록하지 못했습니다.");
        }

        return WithdrawalResult.builder()
                .withdrawalRequestId(order.getId())
                .status(STATUS_COMPLETED)
                .bankTransactionId(transfer.getBankTransactionId())
                .replayed(false)
                .build();
    }

    private WithdrawalResult replayClaimedRequest(
            WithdrawalCommand command,
            String rawKey,
            String ledgerKey,
            DuplicateKeyException duplicate) {
        WithdrawalOrder existing = withdrawalMapper.findByIdempotencyKeyForShare(rawKey);

        if (existing == null) {
            throw new CannotAcquireLockException(
                    "멱등 요청 선점 결과를 확인할 수 없어 재시도가 필요합니다.",
                    duplicate
            );
        }

        validateSameRequest(existing, command);
        validateCompletedOrder(existing);

        WalletTransactionSnapshot snapshot =
                walletMapper.findTransactionByIdempotencyKey(ledgerKey);
        if (!isValidWithdrawalSnapshot(snapshot, existing)) {
            throw new WithdrawalIntegrityException("저장된 출금 원장 스냅샷이 요청과 일치하지 않습니다.");
        }

        return WithdrawalResult.builder()
                .withdrawalRequestId(existing.getId())
                .status(existing.getStatus())
                .bankTransactionId(existing.getMockBankTransactionId())
                .replayed(true)
                .build();

    }

    private void validateSameRequest(WithdrawalOrder existing, WithdrawalCommand command) {
        if (!Objects.equals(existing.getUserId(), command.getUserId())
                || !Objects.equals(
                existing.getLinkedAccountId(), command.getLinkedAccountId())
                || !Objects.equals(existing.getAmount(), command.getAmount())) {
            throw new IdempotencyKeyReusedException(
                    "같은 멱등 키로 다른 출금 요청이 접수되었습니다."
            );
        }
    }

    private void validateCompletedOrder(WithdrawalOrder order) {
        if (!STATUS_COMPLETED.equals(order.getStatus())
                || order.getMockBankTransactionId() == null
                || order.getMockBankTransactionId() <= 0) {
            throw new WithdrawalIntegrityException("완료되지 않은 출금 요청은 재응답할 수 없습니다.");
        }
    }

    private void validateTransferResult(BankTransferResult transfer, Long expectedAmount) {
        if (transfer == null
                || !TRANSFER_SUCCESS.equals(transfer.getStatus())
                || !expectedAmount.equals(transfer.getTransferredAmount())
                || transfer.getBankTransactionId() == null
                || transfer.getBankTransactionId() <= 0
                || transfer.getBankTranId() == null
                || transfer.getBankTranId().isBlank()
                || !isValidDepositBalance(transfer, expectedAmount)) {
            throw new BankTransferIntegrityException("은행 이체 결과가 요청과 일치하지 않습니다.");
        }
    }

    // 출금은 계좌 입금이므로 before + amount == after 여야 한다.
    private boolean isValidDepositBalance(
            BankTransferResult transfer, Long expectedAmount) {
        if (transfer.getBalanceBefore() == null
                || transfer.getBalanceAfter() == null
                || transfer.getBalanceBefore() < 0
                || transfer.getBalanceAfter() < 0) {
            return false;
        }
        try {
            return Math.addExact(
                    transfer.getBalanceBefore(), expectedAmount
            ) == transfer.getBalanceAfter();
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    private boolean isValidWithdrawalSnapshot(
            WalletTransactionSnapshot snapshot, WithdrawalOrder order) {
        if (snapshot == null
                || snapshot.getId() == null
                || snapshot.getId() <= 0
                || !Objects.equals(snapshot.getWalletId(), order.getWalletId())
                || !Objects.equals(snapshot.getWalletUserId(), order.getUserId())
                || snapshot.getWorkCaseId() != null
                || !TX_WITHDRAWAL.equals(snapshot.getTransactionType())
                || !REF_WITHDRAWAL_REQUEST.equals(snapshot.getReferenceType())
                || !Objects.equals(snapshot.getReferenceId(), order.getId())
                || !order.getAmount().equals(snapshot.getAmount())
                || snapshot.getAvailableBefore() == null
                || snapshot.getAvailableBefore() < 0
                || snapshot.getAvailableAfter() == null
                || snapshot.getAvailableAfter() < 0
                || snapshot.getLockedBefore() == null
                || snapshot.getLockedBefore() < 0
                || snapshot.getLockedAfter() == null
                || snapshot.getLockedAfter() < 0
                || !snapshot.getLockedBefore().equals(snapshot.getLockedAfter())) {
            return false;
        }

        try {
            long expectedAfter = Math.subtractExact(
                    snapshot.getAvailableBefore(), order.getAmount()
            );
            return expectedAfter == snapshot.getAvailableAfter().longValue();
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    private void translateInvalidOrderReference(
            WithdrawalCommand command, DataIntegrityViolationException invalidOrder) {
        bankTransferGateway.preflight(BankAccountPreflightCommand.builder()
                .accountId(command.getLinkedAccountId())
                .userId(command.getUserId())
                .build());
        throw new WithdrawalIntegrityException(
                "출금 요청의 참조 무결성을 확인할 수 없습니다.",
                invalidOrder
        );
    }

    private void validateWalletSnapshot(
            WalletBalanceSnapshot wallet, Long expectedUserId, Long claimedWalletId) {
        if (wallet.getWalletId() == null
                || wallet.getWalletId() <= 0
                || !claimedWalletId.equals(wallet.getWalletId())
                || !expectedUserId.equals(wallet.getUserId())
                || wallet.getAvailableBalance() == null
                || wallet.getAvailableBalance() < 0
                || wallet.getLockedBalance() == null
                || wallet.getLockedBalance() < 0) {
            throw new WithdrawalIntegrityException("조회된 지갑 잔액 스냅샷이 올바르지 않습니다.");
        }
    }

    private Long subtractExactly(Long left, Long right, String failureMessage) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException overflow) {
            throw new WithdrawalIntegrityException(failureMessage, overflow);
        }
    }

    private void validateCommand(WithdrawalCommand command) {
        if (command == null
                || command.getUserId() == null
                || command.getUserId() <= 0
                || command.getLinkedAccountId() == null
                || command.getLinkedAccountId() <= 0
                || command.getAmount() == null
                || command.getAmount() <= 0) {
            throw new InvalidWithdrawalRequestException(
                    "출금 요청의 사용자, 계좌, 금액을 확인해 주세요."
            );
        }
    }
}
