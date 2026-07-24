package com.gighub.wallet.service.impl;

import com.gighub.bank.exception.BankTransferIntegrityException;
import com.gighub.bank.service.BankAccountPreflightCommand;
import com.gighub.bank.service.BankTransferCommand;
import com.gighub.bank.service.BankTransferGateway;
import com.gighub.bank.service.BankTransferResult;
import com.gighub.wallet.dto.FundingOrder;
import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.dto.WalletTransactionSnapshot;
import com.gighub.wallet.exception.FundingIntegrityException;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InvalidFundingRequestException;
import com.gighub.wallet.exception.InvalidWalletStateException;
import com.gighub.wallet.idempotency.WalletIdempotencyKeys;
import com.gighub.wallet.mapper.FundingMapper;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.FundingOrderParam;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.wallet.service.FundingService;
import com.gighub.wallet.service.command.FundingCommand;
import com.gighub.wallet.service.result.FundingResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FundingServiceImpl implements FundingService {

    private static final int MAX_TRANSACTION_ATTEMPTS = 3;
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String TRANSFER_SUCCESS = "SUCCESS";
    private static final String TX_FUNDING = "FUNDING";
    private static final String REF_FUNDING_ORDER = "FUNDING_ORDER";

    private final FundingMapper fundingMapper;
    private final WalletMapper walletMapper;
    private final BankTransferGateway bankTransferGateway;
    private final FundingTransactionExecutor transactionExecutor;

    @Override
    public FundingResult fund(FundingCommand command) {
        validateCommand(command);
        String rawKey = WalletIdempotencyKeys.validateRawKey(command.getIdempotencyKey());
        String ledgerKey = WalletIdempotencyKeys.funding(rawKey);

        int attemptCount = 0;
        while (true) {
            attemptCount++;
            try {
                return transactionExecutor.execute(
                        () -> fundOnce(command, rawKey, ledgerKey)
                );
            } catch (PessimisticLockingFailureException retryable) {
                if (attemptCount >= MAX_TRANSACTION_ATTEMPTS) {
                    throw retryable;
                }
            }
        }
    }

    private FundingResult fundOnce(
            FundingCommand command, String rawKey, String ledgerKey) {
        FundingOrderParam order = FundingOrderParam.builder()
                .employerId(command.getEmployerId())
                .linkedAccountId(command.getLinkedAccountId())
                .expectedAmount(command.getAmount())
                .idempotencyKey(rawKey)
                .build();

        try {
            if (fundingMapper.insertFundingOrder(order) != 1
                    || order.getId() == null || order.getId() <= 0) {
                throw new FundingIntegrityException("충전 주문을 선점하지 못했습니다.");
            }
        } catch (DuplicateKeyException duplicate) {
            return replayClaimedOrder(command, rawKey, ledgerKey, duplicate);
        } catch (DataIntegrityViolationException invalidOrder) {
            translateInvalidOrderReference(command, invalidOrder);
        }

        bankTransferGateway.preflight(BankAccountPreflightCommand.builder()
                .accountId(command.getLinkedAccountId())
                .userId(command.getEmployerId())
                .build());

        WalletBalanceSnapshot wallet =
                walletMapper.getWalletSnapshotForUpdate(command.getEmployerId());
        if (wallet == null) {
            throw new InvalidWalletStateException("지갑을 찾을 수 없습니다.");
        }
        validateWalletSnapshot(wallet, command.getEmployerId());
        Long availableAfter = addExactly(
                wallet.getAvailableBalance(),
                command.getAmount(),
                "지갑 충전 후 잔액이 허용 범위를 벗어났습니다."
        );

        BankTransferResult transfer = bankTransferGateway.withdraw(BankTransferCommand.builder()
                .accountId(command.getLinkedAccountId())
                .userId(command.getEmployerId())
                .amount(command.getAmount())
                .referenceType(REF_FUNDING_ORDER)
                .referenceId(order.getId())
                .build());
        validateTransferResult(transfer, command.getAmount());

        if (fundingMapper.completeFundingOrder(
                order.getId(), command.getAmount(), transfer.getBankTransactionId()) != 1) {
            throw new FundingIntegrityException("충전 주문 완료 상태를 기록하지 못했습니다.");
        }

        if (walletMapper.addAvailableBalance(
                command.getEmployerId(), command.getAmount()) != 1) {
            throw new FundingIntegrityException("지갑 충전 잔액을 반영하지 못했습니다.");
        }

        WalletTransactionParam transaction = WalletTransactionParam.builder()
                .walletId(wallet.getWalletId())
                .workCaseId(null)
                .transactionType(TX_FUNDING)
                .amount(command.getAmount())
                .availableBefore(wallet.getAvailableBalance())
                .availableAfter(availableAfter)
                .lockedBefore(wallet.getLockedBalance())
                .lockedAfter(wallet.getLockedBalance())
                .referenceType(REF_FUNDING_ORDER)
                .referenceId(order.getId())
                .idempotencyKey(ledgerKey)
                .build();
        if (walletMapper.insertWalletTransaction(transaction) != 1) {
            throw new FundingIntegrityException("지갑 충전 원장을 기록하지 못했습니다.");
        }

        return FundingResult.builder()
                .fundingOrderId(order.getId())
                .status(STATUS_COMPLETED)
                .bankTransactionId(transfer.getBankTransactionId())
                .availableBalance(availableAfter)
                .lockedBalance(wallet.getLockedBalance())
                .replayed(false)
                .build();
    }

    private FundingResult replayClaimedOrder(
            FundingCommand command,
            String rawKey,
            String ledgerKey,
            DuplicateKeyException duplicate) {
        FundingOrder existing = fundingMapper.findByIdempotencyKeyForShare(rawKey);
        if (existing == null) {
            throw new CannotAcquireLockException(
                    "멱등 요청 선점 결과를 확인할 수 없어 재시도가 필요합니다.",
                    duplicate
            );
        }
        validateSameRequest(existing, command);
        validateCompletedOrder(existing);

        WalletTransactionSnapshot snapshot =
                walletMapper.findFundingTransactionSnapshot(
                        existing.getId(), command.getEmployerId(), ledgerKey);
        if (!isValidFundingSnapshot(snapshot, existing.getExpectedAmount())) {
            throw new FundingIntegrityException("저장된 충전 원장 스냅샷이 주문과 일치하지 않습니다.");
        }

        return FundingResult.builder()
                .fundingOrderId(existing.getId())
                .status(existing.getStatus())
                .bankTransactionId(existing.getMockBankTransactionId())
                .availableBalance(snapshot.getAvailableAfter())
                .lockedBalance(snapshot.getLockedAfter())
                .replayed(true)
                .build();
    }

    private void validateSameRequest(FundingOrder existing, FundingCommand command) {
        if (!Objects.equals(existing.getEmployerId(), command.getEmployerId())
                || !Objects.equals(
                        existing.getLinkedAccountId(), command.getLinkedAccountId())
                || !Objects.equals(existing.getExpectedAmount(), command.getAmount())) {
            throw new IdempotencyKeyReusedException(
                    "같은 멱등 키로 다른 충전 요청이 접수되었습니다."
            );
        }
    }

    private void validateCompletedOrder(FundingOrder order) {
        if (!STATUS_COMPLETED.equals(order.getStatus())
                || order.getMockBankTransactionId() == null
                || order.getMockBankTransactionId() <= 0
                || !order.getExpectedAmount().equals(order.getTransferredAmount())) {
            throw new FundingIntegrityException("완료되지 않은 충전 주문은 재응답할 수 없습니다.");
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
                || !isValidWithdrawalBalance(transfer, expectedAmount)) {
            throw new BankTransferIntegrityException("은행 이체 결과가 요청과 일치하지 않습니다.");
        }
    }

    private boolean isValidWithdrawalBalance(
            BankTransferResult transfer, Long expectedAmount) {
        if (transfer.getBalanceBefore() == null
                || transfer.getBalanceAfter() == null
                || transfer.getBalanceBefore() < 0
                || transfer.getBalanceAfter() < 0) {
            return false;
        }
        try {
            return Math.subtractExact(
                    transfer.getBalanceBefore(), expectedAmount
            ) == transfer.getBalanceAfter();
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    private boolean isValidFundingSnapshot(
            WalletTransactionSnapshot snapshot, Long expectedAmount) {
        if (snapshot == null
                || snapshot.getId() == null
                || snapshot.getId() <= 0
                || snapshot.getWalletId() == null
                || snapshot.getWalletId() <= 0
                || !expectedAmount.equals(snapshot.getAmount())
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
            return Math.addExact(
                    snapshot.getAvailableBefore(), expectedAmount
            ) == snapshot.getAvailableAfter();
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    private void translateInvalidOrderReference(
            FundingCommand command, DataIntegrityViolationException invalidOrder) {
        bankTransferGateway.preflight(BankAccountPreflightCommand.builder()
                .accountId(command.getLinkedAccountId())
                .userId(command.getEmployerId())
                .build());
        throw new FundingIntegrityException(
                "충전 주문의 참조 무결성을 확인할 수 없습니다.",
                invalidOrder
        );
    }

    private void validateWalletSnapshot(
            WalletBalanceSnapshot wallet, Long expectedUserId) {
        if (wallet.getWalletId() == null
                || wallet.getWalletId() <= 0
                || !expectedUserId.equals(wallet.getUserId())
                || wallet.getAvailableBalance() == null
                || wallet.getAvailableBalance() < 0
                || wallet.getLockedBalance() == null
                || wallet.getLockedBalance() < 0) {
            throw new FundingIntegrityException("조회된 지갑 잔액 스냅샷이 올바르지 않습니다.");
        }
    }

    private Long addExactly(Long left, Long right, String failureMessage) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            throw new FundingIntegrityException(failureMessage, overflow);
        }
    }

    private void validateCommand(FundingCommand command) {
        if (command == null
                || command.getEmployerId() == null
                || command.getEmployerId() <= 0
                || command.getLinkedAccountId() == null
                || command.getLinkedAccountId() <= 0
                || command.getAmount() == null
                || command.getAmount() <= 0) {
            throw new InvalidFundingRequestException(
                    "충전 요청의 사용자, 계좌, 금액을 확인해 주세요."
            );
        }
    }
}
