package com.gighub.wallet.service.impl;

import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.dto.WalletTransactionSnapshot;
import com.gighub.wallet.exception.EscrowAccessDeniedException;
import com.gighub.wallet.exception.EscrowIntegrityException;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InsufficientWalletBalanceException;
import com.gighub.wallet.exception.InvalidEscrowStateException;
import com.gighub.wallet.idempotency.WalletIdempotencyKeys;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.wallet.service.EscrowService;
import com.gighub.wallet.service.command.EscrowHoldCommand;
import com.gighub.wallet.service.command.EscrowReleaseCommand;
import com.gighub.work.dto.WorkCaseEscrowContext;
import com.gighub.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    private static final String ESCROW_UNFUNDED = "UNFUNDED";
    private static final String ESCROW_HELD = "HELD";
    private static final String WORK_ACCEPTED = "ACCEPTED";
    private static final String WORK_COMPLETED = "COMPLETED";
    private static final List<String> HOLDABLE_STATUSES = List.of("INVITED");
    private static final List<String> RELEASABLE_STATUSES =
            List.of("ACCEPTED", "READY", "IN_PROGRESS");
    private static final String TX_ESCROW_HOLD = "ESCROW_HOLD";
    private static final String TX_ESCROW_RELEASE = "ESCROW_RELEASE";
    private static final String REF_ESCROW = "ESCROW";

    private final WalletMapper walletMapper;
    private final WorkMapper workMapper;

    @Override
    @Transactional
    public void hold(EscrowHoldCommand command) {
        validateHoldCommand(command);
        String ledgerKey = WalletIdempotencyKeys.escrowHold(command.getIdempotencyKey());

        WorkCaseEscrowContext context =
                workMapper.getEscrowContextForUpdate(command.getWorkCaseId());
        validateHoldContext(context, command);

        WalletTransactionSnapshot existing =
                walletMapper.findTransactionByIdempotencyKey(ledgerKey);
        if (existing != null) {
            validateReplay(
                    existing,
                    context.getEmployerId(),
                    context.getWorkCaseId(),
                    context.getAgreedWage(),
                    TX_ESCROW_HOLD
            );
            validateEscrowReference(
                    existing, requireEscrowId(context.getWorkCaseId()));
            validateHoldLedgerInvariant(existing, context.getAgreedWage());
            return;
        }

        if (!HOLDABLE_STATUSES.contains(context.getStatus())) {
            throw new InvalidEscrowStateException("예치할 수 없는 근무 건 상태입니다.");
        }

        WalletBalanceSnapshot wallet =
                walletMapper.getWalletSnapshotForUpdate(context.getEmployerId());
        validateWalletSnapshot(wallet, context.getEmployerId());
        if (wallet.getAvailableBalance() < context.getAgreedWage()) {
            throw new InsufficientWalletBalanceException(
                    "지갑 잔액이 부족하여 에스크로를 예치할 수 없습니다."
            );
        }
        Long availableAfter = subtractExactly(
                wallet.getAvailableBalance(),
                context.getAgreedWage(),
                "예치 후 사용 가능 잔액이 허용 범위를 벗어났습니다."
        );
        Long lockedAfter = addExactly(
                wallet.getLockedBalance(),
                context.getAgreedWage(),
                "예치 후 잠금 잔액이 허용 범위를 벗어났습니다."
        );

        if (walletMapper.lockEmployerFunds(
                context.getEmployerId(), context.getAgreedWage()) != 1) {
            throw new EscrowIntegrityException("잠금 지갑의 예치 반영 결과가 예상과 다릅니다.");
        }

        String escrowStatus =
                walletMapper.getEscrowStatusForUpdate(context.getWorkCaseId());
        if (escrowStatus == null) {
            if (walletMapper.insertEscrowRecord(
                    context.getWorkCaseId(), context.getAgreedWage()) != 1) {
                throw new EscrowIntegrityException("에스크로 원장을 생성하지 못했습니다.");
            }
        } else if (ESCROW_UNFUNDED.equals(escrowStatus)) {
            if (walletMapper.holdEscrow(context.getWorkCaseId()) != 1) {
                throw new EscrowIntegrityException("에스크로 예치 상태를 반영하지 못했습니다.");
            }
        } else {
            throw new InvalidEscrowStateException("이미 예치된 근무 건입니다.");
        }

        if (workMapper.updateWorkStatus(
                context.getWorkCaseId(), HOLDABLE_STATUSES, WORK_ACCEPTED) != 1) {
            throw new EscrowIntegrityException("근무 건 수락 상태를 반영하지 못했습니다.");
        }

        Long escrowId = requireEscrowId(context.getWorkCaseId());

        WalletTransactionParam transaction = WalletTransactionParam.builder()
                .walletId(wallet.getWalletId())
                .workCaseId(context.getWorkCaseId())
                .transactionType(TX_ESCROW_HOLD)
                .amount(context.getAgreedWage())
                .availableBefore(wallet.getAvailableBalance())
                .availableAfter(availableAfter)
                .lockedBefore(wallet.getLockedBalance())
                .lockedAfter(lockedAfter)
                .referenceType(REF_ESCROW)
                .referenceId(escrowId)
                .idempotencyKey(ledgerKey)
                .build();
        insertWalletTransaction(transaction, "에스크로 예치 원장을 기록하지 못했습니다.");
    }

    @Override
    @Transactional
    public void release(EscrowReleaseCommand command) {
        validateReleaseCommand(command);
        String outKey =
                WalletIdempotencyKeys.escrowReleaseEmployer(command.getIdempotencyKey());
        String inKey =
                WalletIdempotencyKeys.escrowReleaseWorker(command.getIdempotencyKey());

        WorkCaseEscrowContext context =
                workMapper.getEscrowContextForUpdate(command.getWorkCaseId());
        validateReleaseContext(context, command);

        WalletTransactionSnapshot existingOut =
                walletMapper.findTransactionByIdempotencyKey(outKey);
        WalletTransactionSnapshot existingIn =
                walletMapper.findTransactionByIdempotencyKey(inKey);
        if (existingOut != null || existingIn != null) {
            validateReleaseReplay(existingOut, existingIn, context);
            return;
        }

        if (!RELEASABLE_STATUSES.contains(context.getStatus())) {
            throw new InvalidEscrowStateException("정산할 수 없는 근무 건 상태입니다.");
        }

        Map<Long, WalletBalanceSnapshot> wallets =
                lockWalletSnapshotsInOrder(context.getEmployerId(), context.getWorkerId());
        WalletBalanceSnapshot employerWallet = wallets.get(context.getEmployerId());
        WalletBalanceSnapshot workerWallet = wallets.get(context.getWorkerId());

        String escrowStatus =
                walletMapper.getEscrowStatusForUpdate(context.getWorkCaseId());
        if (!ESCROW_HELD.equals(escrowStatus)) {
            throw new InvalidEscrowStateException("정산 가능한 에스크로가 없습니다.");
        }

        Long amount = walletMapper.getHeldEscrowAmount(context.getWorkCaseId());
        if (!context.getAgreedWage().equals(amount)) {
            throw new EscrowIntegrityException("에스크로 금액이 약정 임금과 일치하지 않습니다.");
        }
        if (employerWallet.getLockedBalance() < amount) {
            throw new EscrowIntegrityException("고용주 잠금 잔액이 정산 금액보다 적습니다.");
        }
        Long employerLockedAfter = subtractExactly(
                employerWallet.getLockedBalance(),
                amount,
                "정산 후 고용주 잠금 잔액이 허용 범위를 벗어났습니다."
        );
        Long workerAvailableAfter = addExactly(
                workerWallet.getAvailableBalance(),
                amount,
                "정산 후 근로자 잔액이 허용 범위를 벗어났습니다."
        );

        Long escrowId = requireEscrowId(context.getWorkCaseId());
        validateHeldEscrowOwnership(
                walletMapper.findEscrowHoldTransactionSnapshot(
                        context.getWorkCaseId(), escrowId),
                context,
                escrowId
        );

        if (walletMapper.releaseEscrow(context.getWorkCaseId()) != 1) {
            throw new EscrowIntegrityException("에스크로 정산 상태를 반영하지 못했습니다.");
        }
        if (walletMapper.releaseLockedFunds(context.getEmployerId(), amount) != 1) {
            throw new EscrowIntegrityException("고용주 잠금 잔액을 차감하지 못했습니다.");
        }
        if (walletMapper.addAvailableBalance(context.getWorkerId(), amount) != 1) {
            throw new EscrowIntegrityException("근로자 지갑에 정산금을 반영하지 못했습니다.");
        }
        if (workMapper.updateWorkStatus(
                context.getWorkCaseId(), RELEASABLE_STATUSES, WORK_COMPLETED) != 1) {
            throw new EscrowIntegrityException("근무 건 완료 상태를 반영하지 못했습니다.");
        }

        WalletTransactionParam employerTransaction = WalletTransactionParam.builder()
                .walletId(employerWallet.getWalletId())
                .workCaseId(context.getWorkCaseId())
                .transactionType(TX_ESCROW_RELEASE)
                .amount(amount)
                .availableBefore(employerWallet.getAvailableBalance())
                .availableAfter(employerWallet.getAvailableBalance())
                .lockedBefore(employerWallet.getLockedBalance())
                .lockedAfter(employerLockedAfter)
                .referenceType(REF_ESCROW)
                .referenceId(escrowId)
                .idempotencyKey(outKey)
                .build();
        insertWalletTransaction(employerTransaction, "고용주 정산 원장을 기록하지 못했습니다.");

        WalletTransactionParam workerTransaction = WalletTransactionParam.builder()
                .walletId(workerWallet.getWalletId())
                .workCaseId(context.getWorkCaseId())
                .transactionType(TX_ESCROW_RELEASE)
                .amount(amount)
                .availableBefore(workerWallet.getAvailableBalance())
                .availableAfter(workerAvailableAfter)
                .lockedBefore(workerWallet.getLockedBalance())
                .lockedAfter(workerWallet.getLockedBalance())
                .referenceType(REF_ESCROW)
                .referenceId(escrowId)
                .idempotencyKey(inKey)
                .build();
        insertWalletTransaction(workerTransaction, "근로자 정산 원장을 기록하지 못했습니다.");
    }

    private void insertWalletTransaction(
            WalletTransactionParam transaction, String failureMessage) {
        try {
            if (walletMapper.insertWalletTransaction(transaction) != 1) {
                throw new EscrowIntegrityException(failureMessage);
            }
        } catch (DuplicateKeyException duplicate) {
            throw new IdempotencyKeyReusedException(
                    "같은 멱등 키로 다른 에스크로 요청이 동시에 접수되었습니다."
            );
        }
    }

    private Map<Long, WalletBalanceSnapshot> lockWalletSnapshotsInOrder(
            Long employerId, Long workerId) {
        long firstUserId = Math.min(employerId, workerId);
        long secondUserId = Math.max(employerId, workerId);

        Map<Long, WalletBalanceSnapshot> snapshots = new HashMap<>();
        WalletBalanceSnapshot first =
                walletMapper.getWalletSnapshotForUpdate(firstUserId);
        validateWalletSnapshot(first, firstUserId);
        snapshots.put(firstUserId, first);

        WalletBalanceSnapshot second =
                walletMapper.getWalletSnapshotForUpdate(secondUserId);
        validateWalletSnapshot(second, secondUserId);
        snapshots.put(secondUserId, second);
        return snapshots;
    }

    private void validateHoldCommand(EscrowHoldCommand command) {
        if (command == null
                || command.getWorkCaseId() == null
                || command.getWorkCaseId() <= 0
                || command.getEmployerId() == null
                || command.getEmployerId() <= 0
                || command.getWorkerId() == null
                || command.getWorkerId() <= 0) {
            throw new InvalidEscrowStateException("에스크로 예치 요청 정보를 확인해 주세요.");
        }
    }

    private void validateReleaseCommand(EscrowReleaseCommand command) {
        if (command == null
                || command.getWorkCaseId() == null
                || command.getWorkCaseId() <= 0
                || command.getEmployerId() == null
                || command.getEmployerId() <= 0) {
            throw new InvalidEscrowStateException("에스크로 정산 요청 정보를 확인해 주세요.");
        }
    }

    private void validateHoldContext(
            WorkCaseEscrowContext context, EscrowHoldCommand command) {
        validateContext(context, command.getWorkCaseId());
        if (!context.getEmployerId().equals(command.getEmployerId())
                || !context.getWorkerId().equals(command.getWorkerId())) {
            throw new EscrowAccessDeniedException("근무 건의 계약 당사자가 아닙니다.");
        }
        if (command.getAmount() != null
                && !context.getAgreedWage().equals(command.getAmount())) {
            throw new InvalidEscrowStateException(
                    "요청 금액이 근무 건의 약정 임금과 일치하지 않습니다."
            );
        }
    }

    private void validateReleaseContext(
            WorkCaseEscrowContext context, EscrowReleaseCommand command) {
        validateContext(context, command.getWorkCaseId());
        if (!context.getEmployerId().equals(command.getEmployerId())) {
            throw new EscrowAccessDeniedException("근무 건을 정산할 권한이 없습니다.");
        }
    }

    private void validateContext(
            WorkCaseEscrowContext context, Long expectedWorkCaseId) {
        if (context == null
                || context.getWorkCaseId() == null
                || !context.getWorkCaseId().equals(expectedWorkCaseId)
                || context.getEmployerId() == null
                || context.getEmployerId() <= 0
                || context.getWorkerId() == null
                || context.getWorkerId() <= 0
                || context.getAgreedWage() == null
                || context.getAgreedWage() <= 0) {
            throw new InvalidEscrowStateException("유효한 근무 건 계약 정보를 찾을 수 없습니다.");
        }
        if (context.getEmployerId().equals(context.getWorkerId())) {
            throw new InvalidEscrowStateException(
                    "고용주와 근로자가 동일한 근무 건은 정산할 수 없습니다."
            );
        }
    }

    private void validateWalletSnapshot(
            WalletBalanceSnapshot snapshot, Long expectedUserId) {
        if (snapshot == null) {
            throw new InvalidEscrowStateException("지갑을 찾을 수 없습니다.");
        }
        if (snapshot.getWalletId() == null
                || snapshot.getWalletId() <= 0
                || !expectedUserId.equals(snapshot.getUserId())
                || snapshot.getAvailableBalance() == null
                || snapshot.getAvailableBalance() < 0
                || snapshot.getLockedBalance() == null
                || snapshot.getLockedBalance() < 0) {
            throw new EscrowIntegrityException("조회된 지갑 잔액 스냅샷이 올바르지 않습니다.");
        }
    }

    private void validateReleaseReplay(
            WalletTransactionSnapshot existingOut,
            WalletTransactionSnapshot existingIn,
            WorkCaseEscrowContext context) {
        if (existingOut == null || existingIn == null) {
            throw new EscrowIntegrityException("정산 원장 쌍이 완전하지 않습니다.");
        }
        validateReplay(
                existingOut,
                context.getEmployerId(),
                context.getWorkCaseId(),
                context.getAgreedWage(),
                TX_ESCROW_RELEASE
        );
        validateReplay(
                existingIn,
                context.getWorkerId(),
                context.getWorkCaseId(),
                context.getAgreedWage(),
                TX_ESCROW_RELEASE
        );
        if (!existingOut.getReferenceId().equals(existingIn.getReferenceId())) {
            throw new EscrowIntegrityException("정산 원장 쌍의 에스크로 참조가 일치하지 않습니다.");
        }
        Long escrowId = requireEscrowId(context.getWorkCaseId());
        validateEscrowReference(existingOut, escrowId);
        validateEscrowReference(existingIn, escrowId);
        validateEmployerReleaseLedgerInvariant(existingOut, context.getAgreedWage());
        validateWorkerReleaseLedgerInvariant(existingIn, context.getAgreedWage());
    }

    private void validateReplay(
            WalletTransactionSnapshot snapshot,
            Long walletUserId,
            Long workCaseId,
            Long amount,
            String transactionType) {
        if (snapshot.getId() == null
                || snapshot.getId() <= 0
                || snapshot.getWalletId() == null
                || snapshot.getWalletId() <= 0
                || !walletUserId.equals(snapshot.getWalletUserId())
                || !workCaseId.equals(snapshot.getWorkCaseId())
                || !amount.equals(snapshot.getAmount())
                || !transactionType.equals(snapshot.getTransactionType())
                || !REF_ESCROW.equals(snapshot.getReferenceType())
                || snapshot.getReferenceId() == null
                || snapshot.getReferenceId() <= 0) {
            throw new IdempotencyKeyReusedException(
                    "같은 멱등 키로 다른 에스크로 요청이 접수되었습니다."
            );
        }
    }

    private void validateHoldLedgerInvariant(
            WalletTransactionSnapshot snapshot, Long amount) {
        if (!hasCompleteBalances(snapshot)
                || !matchesSubtract(
                        snapshot.getAvailableBefore(), amount, snapshot.getAvailableAfter())
                || !matchesAdd(
                        snapshot.getLockedBefore(), amount, snapshot.getLockedAfter())) {
            throw new EscrowIntegrityException("저장된 에스크로 예치 원장 잔액이 올바르지 않습니다.");
        }
    }

    private void validateHeldEscrowOwnership(
            WalletTransactionSnapshot snapshot,
            WorkCaseEscrowContext context,
            Long escrowId) {
        if (snapshot == null
                || snapshot.getId() == null
                || snapshot.getId() <= 0
                || snapshot.getWalletId() == null
                || snapshot.getWalletId() <= 0
                || !context.getEmployerId().equals(snapshot.getWalletUserId())
                || !context.getWorkCaseId().equals(snapshot.getWorkCaseId())
                || !context.getAgreedWage().equals(snapshot.getAmount())
                || !TX_ESCROW_HOLD.equals(snapshot.getTransactionType())
                || !REF_ESCROW.equals(snapshot.getReferenceType())
                || !escrowId.equals(snapshot.getReferenceId())) {
            throw new EscrowIntegrityException("예치 원장과 현재 정산 대상의 소유권이 일치하지 않습니다.");
        }
        validateHoldLedgerInvariant(snapshot, context.getAgreedWage());
    }

    private void validateEscrowReference(
            WalletTransactionSnapshot snapshot, Long escrowId) {
        if (!escrowId.equals(snapshot.getReferenceId())) {
            throw new EscrowIntegrityException("원장이 다른 에스크로를 참조하고 있습니다.");
        }
    }

    private Long requireEscrowId(Long workCaseId) {
        Long escrowId = walletMapper.getEscrowIdByWorkCaseId(workCaseId);
        if (escrowId == null || escrowId <= 0) {
            throw new EscrowIntegrityException("근무 건의 에스크로를 찾을 수 없습니다.");
        }
        return escrowId;
    }

    private void validateEmployerReleaseLedgerInvariant(
            WalletTransactionSnapshot snapshot, Long amount) {
        if (!hasCompleteBalances(snapshot)
                || !snapshot.getAvailableBefore().equals(snapshot.getAvailableAfter())
                || !matchesSubtract(
                        snapshot.getLockedBefore(), amount, snapshot.getLockedAfter())) {
            throw new EscrowIntegrityException("저장된 고용주 정산 원장 잔액이 올바르지 않습니다.");
        }
    }

    private void validateWorkerReleaseLedgerInvariant(
            WalletTransactionSnapshot snapshot, Long amount) {
        if (!hasCompleteBalances(snapshot)
                || !matchesAdd(
                        snapshot.getAvailableBefore(), amount, snapshot.getAvailableAfter())
                || !snapshot.getLockedBefore().equals(snapshot.getLockedAfter())) {
            throw new EscrowIntegrityException("저장된 근로자 정산 원장 잔액이 올바르지 않습니다.");
        }
    }

    private boolean hasCompleteBalances(WalletTransactionSnapshot snapshot) {
        return snapshot.getAvailableBefore() != null
                && snapshot.getAvailableBefore() >= 0
                && snapshot.getAvailableAfter() != null
                && snapshot.getAvailableAfter() >= 0
                && snapshot.getLockedBefore() != null
                && snapshot.getLockedBefore() >= 0
                && snapshot.getLockedAfter() != null
                && snapshot.getLockedAfter() >= 0;
    }

    private boolean matchesAdd(Long before, Long amount, Long after) {
        try {
            return Math.addExact(before, amount) == after;
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    private boolean matchesSubtract(Long before, Long amount, Long after) {
        try {
            return Math.subtractExact(before, amount) == after;
        } catch (ArithmeticException overflow) {
            return false;
        }
    }

    private Long addExactly(Long left, Long right, String failureMessage) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            throw new EscrowIntegrityException(failureMessage);
        }
    }

    private Long subtractExactly(Long left, Long right, String failureMessage) {
        try {
            return Math.subtractExact(left, right);
        } catch (ArithmeticException overflow) {
            throw new EscrowIntegrityException(failureMessage);
        }
    }
}
