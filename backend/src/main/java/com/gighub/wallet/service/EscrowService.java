package com.gighub.wallet.service;

import com.gighub.wallet.dto.EscrowLockRequest;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.work.mapper.WorkMapper;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class EscrowService {
    private final WalletMapper walletMapper;
    private final WorkMapper workMapper;

    public EscrowService(WalletMapper walletMapper, WorkMapper workMapper) {
        this.walletMapper = walletMapper;
        this.workMapper = workMapper;
    }

    // 임금 예치 (근로자가 초대를 수락했을 때 호출)
    @Transactional
    public void lockFunds(EscrowLockRequest request){
        // 멱등성 : 이미 처리된 요청(영수증 번호)인지 확인
        if (request.getIdempotencyKey() != null) {
            int isAlreadyProcessed = walletMapper.countTransactionByIdempotencyKey(request.getIdempotencyKey());
            if (isAlreadyProcessed > 0) {
                return;
            }
        }

        // 거래 전 잔액 스냅샷 확보
        BigDecimal availableBefore = walletMapper.getAvailableBalanceForUpdate(request.getEmployerId());
        BigDecimal lockedBefore = walletMapper.getLockedBalance(request.getEmployerId());
        if (lockedBefore == null) lockedBefore = BigDecimal.ZERO; // 널(Null) 방지

        // 잔액 부족 검증
        if (availableBefore == null || availableBefore.compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("지갑 잔액이 부족하여 에스크로를 생성할 수 없습니다.");
        }

        // 사장 지갑 예치 처리 (DB 업데이트)
        int updated = walletMapper.lockEmployerFunds(request.getEmployerId(), request.getAmount());
        if (updated == 0) {
            throw new RuntimeException("지갑 상태 업데이트에 실패했습니다.");
        }

        // ESCROWS에 'HELD' 상태로 인서트
        walletMapper.insertEscrowRecord(request.getWorkCaseId(), request.getAmount(), "HELD");

        // WORK_CASES에 상태 업데이트
        workMapper.updateWorkStatus(request.getWorkCaseId(), "ACCEPTED");

        // 거래 후 잔액 스냅샷 계산 (예치 시 가용잔액 감소, 잠금잔액 증가)
        BigDecimal availableAfter = availableBefore.subtract(request.getAmount());
        BigDecimal lockedAfter = lockedBefore.add(request.getAmount());

        // 원장 불변성 : 돈이 움직였으니 지갑 거래 내역(영수증)을 발행
        Long walletId = walletMapper.getWalletIdByUserId(request.getEmployerId());
        walletMapper.insertWalletTransaction(
                walletId,
                request.getWorkCaseId(),
                "ESCROW_HOLD",
                request.getAmount(),
                request.getIdempotencyKey(),
                availableBefore,
                availableAfter,
                lockedBefore,
                lockedAfter
        );
    }

    // 임금 정산
    @Transactional
    public void releaseFunds(Long workCaseId, Long employerId, String idempotencyKey){
        // 1. 멱등성 : 이미 정산 처리된 요청인지 확인
        if (idempotencyKey != null) {
            int isAlreadyProcessed = walletMapper.countTransactionByIdempotencyKey(idempotencyKey + "_OUT");
            if (isAlreadyProcessed > 0) {
                return;
            }
        }

        Long workerId = workMapper.getWorkerIdByWorkCaseId(workCaseId);
        BigDecimal amount = workMapper.getAgreedWageByWorkCaseId(workCaseId);

        if (workerId == null || amount == null) {
            throw new RuntimeException("유효하지 않은 근무 정보이거나, 알바생이 아직 배정되지 않았습니다.");
        }

        //  사장님과 알바생 양쪽의 거래 전 잔액 스냅샷 확보
        BigDecimal employerAvailableBefore = walletMapper.getAvailableBalanceForUpdate(employerId);
        BigDecimal employerLockedBefore = walletMapper.getLockedBalance(employerId);
        if (employerLockedBefore == null) employerLockedBefore = BigDecimal.ZERO;

        BigDecimal workerAvailableBefore = walletMapper.getAvailableBalanceForUpdate(workerId);
        BigDecimal workerLockedBefore = walletMapper.getLockedBalance(workerId);
        if (workerLockedBefore == null) workerLockedBefore = BigDecimal.ZERO;

        // 사장 잠금 잔액에서 금액 차감
        int employerUpdated = walletMapper.releaseLockedFunds(employerId, amount);
        if (employerUpdated == 0) {
            throw new RuntimeException("사장님의 잠금 잔액을 차감하는 데 실패했습니다.");
        }

        // 알바생의 사용 가능 잔액(available_balance) 증가
        int workerUpdated = walletMapper.addAvailableBalance(workerId, amount);
        if (workerUpdated == 0) {
            throw new RuntimeException("알바생 지갑으로 입금하는 데 실패했습니다.");
        }

        // ESCROWS에 상태를 'RELEASED'로 업데이트
        walletMapper.updateEscrowStatus(workCaseId, "RELEASED");

        // WORK_CASES 상태 업데이트
        workMapper.updateWorkStatus(workCaseId, "COMPLETED");

        // 사장님 원장 기록 (잠금 잔액만 감소, 가용 잔액은 그대로)
        BigDecimal employerLockedAfter = employerLockedBefore.subtract(amount);
        Long employerWalletId = walletMapper.getWalletIdByUserId(employerId);
        walletMapper.insertWalletTransaction(
                employerWalletId, workCaseId, "ESCROW_RELEASE", amount, idempotencyKey + "_OUT",
                employerAvailableBefore, employerAvailableBefore, employerLockedBefore, employerLockedAfter
        );

        // 알바생 원장 기록 (가용 잔액만 증가, 잠금 잔액은 그대로)
        BigDecimal workerAvailableAfter = workerAvailableBefore.add(amount);
        Long workerWalletId = walletMapper.getWalletIdByUserId(workerId);
        walletMapper.insertWalletTransaction(
                workerWalletId, workCaseId, "ESCROW_RELEASE", amount, idempotencyKey + "_IN",
                workerAvailableBefore, workerAvailableAfter, workerLockedBefore, workerLockedBefore
        );
    }
}