package com.gighub.wallet.service;

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
import com.gighub.wallet.service.command.EscrowHoldCommand;
import com.gighub.wallet.service.command.EscrowReleaseCommand;
import com.gighub.wallet.service.impl.EscrowServiceImpl;
import com.gighub.work.dto.WorkCaseEscrowContext;
import com.gighub.work.mapper.WorkMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EscrowServiceTest {

    private static final Long EMPLOYER_ID = 3L;
    private static final Long WORKER_ID = 4L;
    private static final Long WORK_CASE_ID = 1L;
    private static final Long AGREED_WAGE = 300_000L;
    private static final String KEY = "TEST-KEY-001";
    private static final List<String> HOLDABLE = List.of("INVITED");
    private static final List<String> RELEASABLE =
            List.of("ACCEPTED", "READY", "IN_PROGRESS");

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WorkMapper workMapper;

    @InjectMocks
    private EscrowServiceImpl escrowService;

    @Test
    @DisplayName("예치는 잠긴 단일 지갑 스냅샷으로 잔액과 원장을 기록한다")
    void holdUsesSingleLockedWalletSnapshot() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "INVITED");
        WalletBalanceSnapshot wallet =
                wallet(30L, EMPLOYER_ID, 700_000L, 0L);

        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID)).thenReturn(wallet);
        when(walletMapper.lockEmployerFunds(EMPLOYER_ID, AGREED_WAGE)).thenReturn(1);
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID)).thenReturn(null);
        when(walletMapper.insertEscrowRecord(WORK_CASE_ID, AGREED_WAGE)).thenReturn(1);
        when(workMapper.updateWorkStatus(WORK_CASE_ID, HOLDABLE, "ACCEPTED")).thenReturn(1);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);
        when(walletMapper.insertWalletTransaction(any())).thenReturn(1);

        assertDoesNotThrow(() -> escrowService.hold(holdCommand(AGREED_WAGE)));

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper).insertWalletTransaction(captor.capture());
        WalletTransactionParam transaction = captor.getValue();

        assertEquals(30L, transaction.getWalletId());
        assertEquals(700_000L, transaction.getAvailableBefore());
        assertEquals(400_000L, transaction.getAvailableAfter());
        assertEquals(0L, transaction.getLockedBefore());
        assertEquals(300_000L, transaction.getLockedAfter());
        assertEquals(
                WalletIdempotencyKeys.escrowHold(KEY),
                transaction.getIdempotencyKey()
        );

        InOrder lockOrder = inOrder(workMapper, walletMapper);
        lockOrder.verify(workMapper).getEscrowContextForUpdate(WORK_CASE_ID);
        lockOrder.verify(walletMapper).getWalletSnapshotForUpdate(EMPLOYER_ID);
        lockOrder.verify(walletMapper).getEscrowStatusForUpdate(WORK_CASE_ID);
        verify(walletMapper, never()).getAvailableBalance(anyLong());
        verify(walletMapper, never()).getLockedBalance(anyLong());
        verify(walletMapper, never()).getWalletIdByUserId(anyLong());
    }

    @Test
    @DisplayName("예치 잔액이 부족하면 어떤 상태도 변경하지 않는다")
    void holdRejectsInsufficientSnapshotBalance() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context(EMPLOYER_ID, WORKER_ID, "INVITED"));
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID))
                .thenReturn(wallet(30L, EMPLOYER_ID, 30_000L, 0L));

        assertThrows(
                InsufficientWalletBalanceException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).lockEmployerFunds(anyLong(), anyLong());
        verify(walletMapper, never()).insertEscrowRecord(anyLong(), anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    @DisplayName("손상된 지갑 스냅샷은 예치 전 서버 무결성 오류로 거부한다")
    void holdRejectsCorruptedWalletSnapshot() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context(EMPLOYER_ID, WORKER_ID, "INVITED"));
        WalletBalanceSnapshot corrupted =
                wallet(30L, EMPLOYER_ID, 700_000L, 0L);
        corrupted.setUserId(WORKER_ID);
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID))
                .thenReturn(corrupted);

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).lockEmployerFunds(anyLong(), anyLong());
        verify(walletMapper, never()).insertEscrowRecord(anyLong(), anyLong());
    }

    @Test
    @DisplayName("잠금 지갑의 예치 UPDATE가 0건이면 서버 무결성 오류다")
    void holdRejectsUnexpectedWalletUpdateCount() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context(EMPLOYER_ID, WORKER_ID, "INVITED"));
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID))
                .thenReturn(wallet(30L, EMPLOYER_ID, 700_000L, 0L));
        when(walletMapper.lockEmployerFunds(EMPLOYER_ID, AGREED_WAGE)).thenReturn(0);

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).insertEscrowRecord(anyLong(), anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    @DisplayName("예치는 근무 건의 실제 고용주와 요청 고용주가 다르면 거부한다")
    void holdRejectsEmployerMismatch() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context(99L, WORKER_ID, "INVITED"));

        assertThrows(
                EscrowAccessDeniedException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    @DisplayName("동일한 예치 요청은 범위 키의 저장 원장을 확인하고 재처리하지 않는다")
    void holdReplaysMatchingLedger() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "ACCEPTED");
        String key = WalletIdempotencyKeys.escrowHold(KEY);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.findTransactionByIdempotencyKey(key))
                .thenReturn(transaction(EMPLOYER_ID, WORK_CASE_ID, AGREED_WAGE,
                        "ESCROW_HOLD"));
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertDoesNotThrow(() -> escrowService.hold(holdCommand(AGREED_WAGE)));

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    @DisplayName("저장된 예치 원장의 잔액 증감이 틀리면 재응답하지 않는다")
    void holdRejectsInvalidReplayBalance() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "ACCEPTED");
        String key = WalletIdempotencyKeys.escrowHold(KEY);
        WalletTransactionSnapshot replay =
                transaction(EMPLOYER_ID, WORK_CASE_ID, AGREED_WAGE, "ESCROW_HOLD");
        replay.setLockedAfter(AGREED_WAGE - 1);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.findTransactionByIdempotencyKey(key)).thenReturn(replay);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    @DisplayName("예치 replay 원장이 다른 에스크로 ID를 가리키면 거부한다")
    void holdRejectsDifferentEscrowReference() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "ACCEPTED");
        String key = WalletIdempotencyKeys.escrowHold(KEY);
        WalletTransactionSnapshot replay =
                transaction(EMPLOYER_ID, WORK_CASE_ID, AGREED_WAGE, "ESCROW_HOLD");
        replay.setReferenceId(99L);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.findTransactionByIdempotencyKey(key)).thenReturn(replay);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );
    }

    @Test
    @DisplayName("같은 예치 멱등 키를 다른 근무 건에 사용하면 충돌로 거부한다")
    void holdRejectsReusedKeyForDifferentWorkCase() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "INVITED");
        String key = WalletIdempotencyKeys.escrowHold(KEY);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.findTransactionByIdempotencyKey(key))
                .thenReturn(transaction(EMPLOYER_ID, 99L, AGREED_WAGE, "ESCROW_HOLD"));

        assertThrows(
                IdempotencyKeyReusedException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );
    }

    @Test
    @DisplayName("정산은 두 지갑을 사용자 ID 오름차순으로 잠그고 역할별 원장을 기록한다")
    void releaseLocksWalletsInOrderAndRecordsBothLedgers() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "ACCEPTED");
        WalletBalanceSnapshot employer =
                wallet(30L, EMPLOYER_ID, 400_000L, 300_000L);
        WalletBalanceSnapshot worker =
                wallet(40L, WORKER_ID, 0L, 0L);
        stubRelease(context, employer, worker);

        assertDoesNotThrow(() -> escrowService.release(releaseCommand(EMPLOYER_ID)));

        InOrder lockOrder = inOrder(workMapper, walletMapper);
        lockOrder.verify(workMapper).getEscrowContextForUpdate(WORK_CASE_ID);
        lockOrder.verify(walletMapper).getWalletSnapshotForUpdate(EMPLOYER_ID);
        lockOrder.verify(walletMapper).getWalletSnapshotForUpdate(WORKER_ID);
        lockOrder.verify(walletMapper).getEscrowStatusForUpdate(WORK_CASE_ID);

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper, times(2)).insertWalletTransaction(captor.capture());
        WalletTransactionParam out = captor.getAllValues().get(0);
        WalletTransactionParam in = captor.getAllValues().get(1);

        assertEquals(30L, out.getWalletId());
        assertEquals(400_000L, out.getAvailableAfter());
        assertEquals(0L, out.getLockedAfter());
        assertEquals(
                WalletIdempotencyKeys.escrowReleaseEmployer(KEY),
                out.getIdempotencyKey()
        );
        assertEquals(40L, in.getWalletId());
        assertEquals(300_000L, in.getAvailableAfter());
        assertEquals(
                WalletIdempotencyKeys.escrowReleaseWorker(KEY),
                in.getIdempotencyKey()
        );
    }

    @Test
    @DisplayName("고용주 ID가 더 커도 잠금 순서와 원장 역할을 혼동하지 않는다")
    void releaseMapsRolesAfterReverseIdLockOrder() {
        Long employerId = 8L;
        Long workerId = 2L;
        WorkCaseEscrowContext context =
                context(employerId, workerId, "READY");
        WalletBalanceSnapshot employer =
                wallet(80L, employerId, 10_000L, AGREED_WAGE);
        WalletBalanceSnapshot worker =
                wallet(20L, workerId, 50_000L, 0L);
        stubRelease(context, worker, employer);

        assertDoesNotThrow(() -> escrowService.release(releaseCommand(employerId)));

        InOrder lockOrder = inOrder(walletMapper);
        lockOrder.verify(walletMapper).getWalletSnapshotForUpdate(workerId);
        lockOrder.verify(walletMapper).getWalletSnapshotForUpdate(employerId);

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper, times(2)).insertWalletTransaction(captor.capture());
        assertEquals(80L, captor.getAllValues().get(0).getWalletId());
        assertEquals(20L, captor.getAllValues().get(1).getWalletId());
    }

    @Test
    @DisplayName("근무 건 고용주가 아닌 사용자는 지갑 잠금 전에 정산이 거부된다")
    void releaseRejectsUnauthorizedEmployer() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context(99L, WORKER_ID, "ACCEPTED"));

        assertThrows(
                EscrowAccessDeniedException.class,
                () -> escrowService.release(releaseCommand(EMPLOYER_ID))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    @DisplayName("고용주와 근로자가 같은 근무 건은 정산하지 않는다")
    void releaseRejectsSameParticipant() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context(EMPLOYER_ID, EMPLOYER_ID, "ACCEPTED"));

        assertThrows(
                InvalidEscrowStateException.class,
                () -> escrowService.release(releaseCommand(EMPLOYER_ID))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    @DisplayName("예치 이후 고용주가 바뀌면 다른 고용주의 잠금 잔액으로 정산하지 않는다")
    void releaseRejectsChangedEmployerAfterHold() {
        Long changedEmployerId = 8L;
        WorkCaseEscrowContext context =
                context(changedEmployerId, WORKER_ID, "ACCEPTED");
        WalletBalanceSnapshot worker =
                wallet(40L, WORKER_ID, 0L, 0L);
        WalletBalanceSnapshot changedEmployer =
                wallet(80L, changedEmployerId, 0L, AGREED_WAGE);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.getWalletSnapshotForUpdate(WORKER_ID)).thenReturn(worker);
        when(walletMapper.getWalletSnapshotForUpdate(changedEmployerId))
                .thenReturn(changedEmployer);
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID)).thenReturn("HELD");
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);
        when(walletMapper.findEscrowHoldTransactionSnapshot(WORK_CASE_ID, 11L))
                .thenReturn(transaction(
                        EMPLOYER_ID, WORK_CASE_ID, AGREED_WAGE, "ESCROW_HOLD"));

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.release(releaseCommand(changedEmployerId))
        );

        verify(walletMapper, never()).releaseEscrow(anyLong());
        verify(walletMapper, never()).releaseLockedFunds(anyLong(), anyLong());
        verify(walletMapper, never()).addAvailableBalance(anyLong(), anyLong());
    }

    @Test
    @DisplayName("정산 재요청은 고용주와 근로자 원장 쌍이 모두 있어야 성공한다")
    void releaseRejectsIncompleteReplayPair() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "COMPLETED");
        String outKey = WalletIdempotencyKeys.escrowReleaseEmployer(KEY);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.findTransactionByIdempotencyKey(outKey))
                .thenReturn(transaction(EMPLOYER_ID, WORK_CASE_ID, AGREED_WAGE,
                        "ESCROW_RELEASE"));

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.release(releaseCommand(EMPLOYER_ID))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    @DisplayName("완전한 정산 원장 쌍은 지갑을 다시 변경하지 않고 재응답한다")
    void releaseReplaysCompleteLedgerPair() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "COMPLETED");
        String outKey = WalletIdempotencyKeys.escrowReleaseEmployer(KEY);
        String inKey = WalletIdempotencyKeys.escrowReleaseWorker(KEY);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.findTransactionByIdempotencyKey(outKey))
                .thenReturn(transaction(
                        EMPLOYER_ID, WORK_CASE_ID, AGREED_WAGE, "ESCROW_RELEASE"));
        when(walletMapper.findTransactionByIdempotencyKey(inKey))
                .thenReturn(transaction(
                        WORKER_ID, WORK_CASE_ID, AGREED_WAGE, "ESCROW_RELEASE"));
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertDoesNotThrow(() -> escrowService.release(releaseCommand(EMPLOYER_ID)));

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
        verify(walletMapper, never()).releaseEscrow(anyLong());
    }

    @Test
    @DisplayName("동시에 충돌한 정산 원장 키는 409용 멱등 키 재사용 오류로 변환한다")
    void releaseTranslatesConcurrentLedgerCollision() {
        WorkCaseEscrowContext context =
                context(EMPLOYER_ID, WORKER_ID, "ACCEPTED");
        WalletBalanceSnapshot employer =
                wallet(30L, EMPLOYER_ID, 400_000L, 300_000L);
        WalletBalanceSnapshot worker =
                wallet(40L, WORKER_ID, 0L, 0L);
        stubRelease(context, employer, worker);
        when(walletMapper.insertWalletTransaction(any()))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("concurrent ledger"));

        assertThrows(
                IdempotencyKeyReusedException.class,
                () -> escrowService.release(releaseCommand(EMPLOYER_ID))
        );

        verify(walletMapper, times(2)).insertWalletTransaction(any());
    }

    private void stubRelease(
            WorkCaseEscrowContext context,
            WalletBalanceSnapshot first,
            WalletBalanceSnapshot second) {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.getWalletSnapshotForUpdate(first.getUserId())).thenReturn(first);
        when(walletMapper.getWalletSnapshotForUpdate(second.getUserId())).thenReturn(second);
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID)).thenReturn("HELD");
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);
        when(walletMapper.findEscrowHoldTransactionSnapshot(WORK_CASE_ID, 11L))
                .thenReturn(transaction(
                        context.getEmployerId(),
                        WORK_CASE_ID,
                        AGREED_WAGE,
                        "ESCROW_HOLD"
                ));
        when(walletMapper.releaseEscrow(WORK_CASE_ID)).thenReturn(1);
        when(walletMapper.releaseLockedFunds(context.getEmployerId(), AGREED_WAGE))
                .thenReturn(1);
        when(walletMapper.addAvailableBalance(context.getWorkerId(), AGREED_WAGE))
                .thenReturn(1);
        when(workMapper.updateWorkStatus(WORK_CASE_ID, RELEASABLE, "COMPLETED"))
                .thenReturn(1);
        when(walletMapper.insertWalletTransaction(any())).thenReturn(1);
    }

    private EscrowHoldCommand holdCommand(Long amount) {
        return EscrowHoldCommand.builder()
                .employerId(EMPLOYER_ID)
                .workerId(WORKER_ID)
                .workCaseId(WORK_CASE_ID)
                .amount(amount)
                .idempotencyKey(KEY)
                .build();
    }

    private EscrowReleaseCommand releaseCommand(Long employerId) {
        return EscrowReleaseCommand.builder()
                .employerId(employerId)
                .workCaseId(WORK_CASE_ID)
                .idempotencyKey(KEY)
                .build();
    }

    private WorkCaseEscrowContext context(
            Long employerId, Long workerId, String status) {
        WorkCaseEscrowContext context = new WorkCaseEscrowContext();
        context.setWorkCaseId(WORK_CASE_ID);
        context.setEmployerId(employerId);
        context.setWorkerId(workerId);
        context.setAgreedWage(AGREED_WAGE);
        context.setStatus(status);
        return context;
    }

    private WalletBalanceSnapshot wallet(
            Long walletId,
            Long userId,
            Long availableBalance,
            Long lockedBalance) {
        WalletBalanceSnapshot wallet = new WalletBalanceSnapshot();
        wallet.setWalletId(walletId);
        wallet.setUserId(userId);
        wallet.setAvailableBalance(availableBalance);
        wallet.setLockedBalance(lockedBalance);
        return wallet;
    }

    private WalletTransactionSnapshot transaction(
            Long walletUserId,
            Long workCaseId,
            Long amount,
            String transactionType) {
        WalletTransactionSnapshot transaction = new WalletTransactionSnapshot();
        transaction.setId(1L);
        transaction.setWalletId(30L);
        transaction.setWalletUserId(walletUserId);
        transaction.setWorkCaseId(workCaseId);
        transaction.setTransactionType(transactionType);
        transaction.setAmount(amount);
        if ("ESCROW_HOLD".equals(transactionType)) {
            transaction.setAvailableBefore(700_000L);
            transaction.setAvailableAfter(400_000L);
            transaction.setLockedBefore(0L);
            transaction.setLockedAfter(300_000L);
        } else if (EMPLOYER_ID.equals(walletUserId)) {
            transaction.setAvailableBefore(400_000L);
            transaction.setAvailableAfter(400_000L);
            transaction.setLockedBefore(300_000L);
            transaction.setLockedAfter(0L);
        } else {
            transaction.setAvailableBefore(0L);
            transaction.setAvailableAfter(300_000L);
            transaction.setLockedBefore(0L);
            transaction.setLockedAfter(0L);
        }
        transaction.setReferenceType("ESCROW");
        transaction.setReferenceId(11L);
        return transaction;
    }
}
