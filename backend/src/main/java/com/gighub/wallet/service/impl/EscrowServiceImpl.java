package com.gighub.wallet.service.impl;

import com.gighub.wallet.exception.InsufficientWalletBalanceException;
import com.gighub.wallet.exception.InvalidEscrowStateException;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.wallet.service.EscrowService;
import com.gighub.wallet.service.command.EscrowHoldCommand;
import com.gighub.wallet.service.command.EscrowReleaseCommand;
import com.gighub.work.mapper.WorkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EscrowServiceImpl implements EscrowService {

    /** escrows.status (ck_escrows_status) */
    private static final String ESCROW_UNFUNDED = "UNFUNDED";

    /** work_cases.status (ck_work_cases_status) */
    private static final String WORK_ACCEPTED = "ACCEPTED";
    private static final String WORK_COMPLETED = "COMPLETED";
    private static final List<String> HOLDABLE_STATUSES = List.of("INVITED");
    private static final List<String> RELEASABLE_STATUSES =
            List.of("ACCEPTED", "READY", "IN_PROGRESS");

    /** wallet_transactions.transaction_type (ck_wallet_transactions_type) */
    private static final String TX_ESCROW_HOLD = "ESCROW_HOLD";
    private static final String TX_ESCROW_RELEASE = "ESCROW_RELEASE";

    private static final String REF_ESCROW = "ESCROW";
    private static final String KEY_SUFFIX_OUT = "_OUT";
    private static final String KEY_SUFFIX_IN = "_IN";

    private final WalletMapper walletMapper;
    private final WorkMapper workMapper;

    @Override
    @Transactional
    public void hold(EscrowHoldCommand command) {
        // idempotency_key는 NOT NULL UNIQUE이므로 컨트롤러에서 필수 헤더로 보장한다.
        if (walletMapper.countTransactionByIdempotencyKey(command.getIdempotencyKey()) > 0) {
            return;
        }

        Long workCaseId = command.getWorkCaseId();
        Long employerId = command.getEmployerId();

        // 금액은 클라이언트 값이 아니라 agreed_wage에서 도출한다.
        // fk_escrows_case_wage가 (work_case_id, amount) = (id, agreed_wage)를 강제한다.
        Long amount = workMapper.getAgreedWageByWorkCaseId(workCaseId);
        if (amount == null) {
            throw new InvalidEscrowStateException("존재하지 않는 근무 건입니다.");
        }

        if(command.getAmount() != null && !amount.equals(command.getAmount())){
            throw new InvalidEscrowStateException("여청 금액이 약정 임금과 일치하지 않습니다.");
        }

        // ck_work_cases_matched_worker: ACCEPTED 전이에는 worker_id가 필요하다.
        Long assignedWorkerId = workMapper.getWorkerIdByWorkCaseId(workCaseId);
        if (assignedWorkerId == null || !assignedWorkerId.equals(command.getWorkerId())) {
            throw new InvalidEscrowStateException("근무 건에 배정된 알바생이 아닙니다.");
        }

        // 스냅샷은 반드시 UPDATE 이전에 확보한다.
        Long availableBefore = walletMapper.getAvailableBalanceForUpdate(employerId);
        if (availableBefore == null) {
            throw new InvalidEscrowStateException("고용주 지갑을 찾을 수 없습니다.");
        }
        if (availableBefore < amount) {
            throw new InsufficientWalletBalanceException("지갑 잔액이 부족하여 에스크로를 생성할 수 없습니다.");
        }
        Long lockedBefore = walletMapper.getLockedBalance(employerId);
        Long walletId = walletMapper.getWalletIdByUserId(employerId);
        if (walletId == null) {
            throw new InvalidEscrowStateException("고용주 지갑을 찾을 수 없습니다.");
        }

        if (walletMapper.lockEmployerFunds(employerId, amount) != 1) {
            throw new InsufficientWalletBalanceException("지갑 잔액이 부족하여 에스크로를 생성할 수 없습니다.");
        }

        // escrows는 work_case당 1행(uk_escrows_work_case_id).
        // 선행 행이 있으면 UNFUNDED 상태에서만 HELD로 전이한다.
        String escrowStatus = walletMapper.getEscrowStatusForUpdate(workCaseId);
        if (escrowStatus == null) {
            walletMapper.insertEscrowRecord(workCaseId, amount);
        } else if (ESCROW_UNFUNDED.equals(escrowStatus)) {
            if (walletMapper.holdEscrow(workCaseId) != 1) {
                throw new InvalidEscrowStateException("에스크로 상태 전이에 실패했습니다.");
            }
        } else {
            throw new InvalidEscrowStateException("이미 예치된 근무 건입니다.");
        }

        if (workMapper.updateWorkStatus(workCaseId, HOLDABLE_STATUSES, WORK_ACCEPTED) != 1) {
            throw new InvalidEscrowStateException("수락할 수 없는 근무 건 상태입니다.");
        }

        Long escrowId = walletMapper.getEscrowIdByWorkCaseId(workCaseId);

        // 예치: available 감소, locked 증가
        walletMapper.insertWalletTransaction(WalletTransactionParam.builder()
                .walletId(walletId)
                .workCaseId(workCaseId)
                .transactionType(TX_ESCROW_HOLD)
                .amount(amount)
                .availableBefore(availableBefore)
                .availableAfter(availableBefore - amount)
                .lockedBefore(lockedBefore)
                .lockedAfter(lockedBefore + amount)
                .referenceType(REF_ESCROW)
                .referenceId(escrowId)
                .idempotencyKey(command.getIdempotencyKey())
                .build());
    }

    @Override
    @Transactional
    public void release(EscrowReleaseCommand command) {
        String outKey = command.getIdempotencyKey() + KEY_SUFFIX_OUT;
        String inKey = command.getIdempotencyKey() + KEY_SUFFIX_IN;
        if (walletMapper.countTransactionByIdempotencyKey(outKey) > 0) {
            return;
        }

        Long workCaseId = command.getWorkCaseId();
        Long employerId = command.getEmployerId();

        // 수령자와 금액은 서버가 도출한다.
        Long workerId = workMapper.getWorkerIdByWorkCaseId(workCaseId);
        if (workerId == null) {
            throw new InvalidEscrowStateException("알바생이 배정되지 않은 근무 건입니다.");
        }
        Long amount = walletMapper.getHeldEscrowAmount(workCaseId);
        if (amount == null) {
            throw new InvalidEscrowStateException("정산 가능한 에스크로(HELD)가 없습니다.");
        }
        // TODO(후속): work_cases.employer_id == employerId 소유권 검증

        lockWalletsInOrder(employerId, workerId);

        // 양쪽 스냅샷 확보 (행은 이미 잠겨 있다)
        Long empAvailable = walletMapper.getAvailableBalance(employerId);
        Long empLocked = walletMapper.getLockedBalance(employerId);
        Long empWalletId = walletMapper.getWalletIdByUserId(employerId);
        Long wrkAvailable = walletMapper.getAvailableBalance(workerId);
        Long wrkLocked = walletMapper.getLockedBalance(workerId);
        Long wrkWalletId = walletMapper.getWalletIdByUserId(workerId);
        if (empWalletId == null || wrkWalletId == null) {
            throw new InvalidEscrowStateException("지갑이 존재하지 않습니다.");
        }

        Long escrowId = walletMapper.getEscrowIdByWorkCaseId(workCaseId);

        // HELD -> RELEASED 전이 실패 = 이미 정산된 건 (멱등 키가 달라도 차단)
        if (walletMapper.releaseEscrow(workCaseId) != 1) {
            throw new InvalidEscrowStateException("이미 정산되었거나 정산할 수 없는 상태입니다.");
        }
        if (walletMapper.releaseLockedFunds(employerId, amount) != 1) {
            throw new InvalidEscrowStateException("고용주의 잠금 잔액이 부족합니다.");
        }
        if (walletMapper.addAvailableBalance(workerId, amount) != 1) {
            throw new InvalidEscrowStateException("알바생 지갑 입금에 실패했습니다.");
        }
        if (workMapper.updateWorkStatus(workCaseId, RELEASABLE_STATUSES, WORK_COMPLETED) != 1) {
            throw new InvalidEscrowStateException("정산할 수 없는 근무 건 상태입니다.");
        }

        // 고용주: locked만 감소, available 불변
        walletMapper.insertWalletTransaction(WalletTransactionParam.builder()
                .walletId(empWalletId)
                .workCaseId(workCaseId)
                .transactionType(TX_ESCROW_RELEASE)
                .amount(amount)
                .availableBefore(empAvailable)
                .availableAfter(empAvailable)
                .lockedBefore(empLocked)
                .lockedAfter(empLocked - amount)
                .referenceType(REF_ESCROW)
                .referenceId(escrowId)
                .idempotencyKey(outKey)
                .build());

        // 알바생: available만 증가, locked 불변
        walletMapper.insertWalletTransaction(WalletTransactionParam.builder()
                .walletId(wrkWalletId)
                .workCaseId(workCaseId)
                .transactionType(TX_ESCROW_RELEASE)
                .amount(amount)
                .availableBefore(wrkAvailable)
                .availableAfter(wrkAvailable + amount)
                .lockedBefore(wrkLocked)
                .lockedAfter(wrkLocked)
                .referenceType(REF_ESCROW)
                .referenceId(escrowId)
                .idempotencyKey(inKey)
                .build());
    }

    /** user_id 오름차순으로 잠가 상호 거래 시 데드락을 피한다. */
    private void lockWalletsInOrder(Long userIdA, Long userIdB) {
        long first = Math.min(userIdA, userIdB);
        long second = Math.max(userIdA, userIdB);
        walletMapper.getAvailableBalanceForUpdate(first);
        if (first != second) {
            walletMapper.getAvailableBalanceForUpdate(second);
        }
    }
}