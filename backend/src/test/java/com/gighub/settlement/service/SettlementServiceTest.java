package com.gighub.settlement.service;

import com.gighub.settlement.domain.SettlementStatus;
import com.gighub.settlement.dto.SettlementSnapshot;
import com.gighub.settlement.mapper.SettlementLedgerMapper;
import com.gighub.settlement.mapper.SettlementMapper;
import com.gighub.settlement.service.command.SettlementApproveCommand;
import com.gighub.settlement.service.impl.SettlementServiceImpl;
import com.gighub.settlement.service.result.SettlementResult;
import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.dto.WalletTransactionSnapshot;
import com.gighub.wallet.exception.EscrowAccessDeniedException;
import com.gighub.wallet.exception.EscrowIntegrityException;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InvalidEscrowStateException;
import com.gighub.wallet.idempotency.WalletIdempotencyKeys;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.work.dto.WorkCaseEscrowContext;
import com.gighub.work.mapper.WorkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    private static final Long EMPLOYER_ID = 3L;
    private static final Long WORKER_ID = 4L;
    private static final Long WORK_CASE_ID = 1L;
    private static final Long SETTLEMENT_ID = 12L;
    private static final Long ESCROW_ID = 11L;
    private static final Long AGREED_WAGE = 300_000L;
    private static final String KEY = "SETTLEMENT-KEY-001";
    private static final List<String> RELEASABLE =
            List.of("ACCEPTED", "READY", "IN_PROGRESS");
    private static final LocalDateTime PROCESSING_AT =
            LocalDateTime.of(2026, 7, 24, 17, 10);
    private static final LocalDateTime COMPLETED_AT =
            LocalDateTime.of(2026, 7, 24, 17, 10, 1, 123_456_000);

    @Mock
    private SettlementMapper settlementMapper;

    @Mock
    private SettlementLedgerMapper settlementLedgerMapper;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WorkMapper workMapper;

    @InjectMocks
    private SettlementServiceImpl settlementService;

    @Test
    void approveTransitionsAllStateAndReturnsStoredCompletionSnapshot() {
        stubNewApproval(context("ACCEPTED"));

        SettlementResult result = settlementService.approve(command(EMPLOYER_ID));

        assertEquals(SETTLEMENT_ID, result.getSettlementId());
        assertEquals("COMPLETED", result.getStatus());
        assertEquals(COMPLETED_AT, result.getCompletedAt());
        assertFalse(result.isReplayed());

        InOrder order = inOrder(workMapper, settlementMapper, walletMapper);
        order.verify(workMapper).getEscrowContextForUpdate(WORK_CASE_ID);
        order.verify(settlementMapper).findByWorkCaseIdForUpdate(WORK_CASE_ID);
        order.verify(settlementMapper)
                .transitionWaitingToProcessing(SETTLEMENT_ID, EMPLOYER_ID);
        order.verify(walletMapper).getWalletSnapshotForUpdate(EMPLOYER_ID);
        order.verify(walletMapper).getWalletSnapshotForUpdate(WORKER_ID);
        order.verify(walletMapper).getEscrowStatusForUpdate(WORK_CASE_ID);
        order.verify(settlementMapper)
                .transitionProcessingToCompleted(SETTLEMENT_ID, EMPLOYER_ID);
        order.verify(settlementMapper).findByWorkCaseIdForUpdate(WORK_CASE_ID);

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper, times(2)).insertWalletTransaction(captor.capture());
        WalletTransactionParam employerLedger = captor.getAllValues().get(0);
        WalletTransactionParam workerLedger = captor.getAllValues().get(1);
        assertEquals(0L, employerLedger.getLockedAfter());
        assertEquals(
                WalletIdempotencyKeys.escrowReleaseEmployer(KEY),
                employerLedger.getIdempotencyKey()
        );
        assertEquals(AGREED_WAGE, workerLedger.getAvailableAfter());
        assertEquals(
                WalletIdempotencyKeys.escrowReleaseWorker(KEY),
                workerLedger.getIdempotencyKey()
        );
    }

    @Test
    void approveReplaysOnlyCompleteSettlementAndLedgerPair() {
        WorkCaseEscrowContext context = context("COMPLETED");
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context);
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement(SettlementStatus.COMPLETED));
        when(settlementLedgerMapper.findByIdempotencyKeyForShare(
                WalletIdempotencyKeys.escrowReleaseEmployer(KEY)))
                .thenReturn(releaseLedger(EMPLOYER_ID));
        when(settlementLedgerMapper.findByIdempotencyKeyForShare(
                WalletIdempotencyKeys.escrowReleaseWorker(KEY)))
                .thenReturn(releaseLedger(WORKER_ID));
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID))
                .thenReturn(ESCROW_ID);
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID))
                .thenReturn("RELEASED");

        SettlementResult replay =
                settlementService.approve(command(EMPLOYER_ID));

        assertEquals(SETTLEMENT_ID, replay.getSettlementId());
        assertEquals(COMPLETED_AT, replay.getCompletedAt());
        assertTrue(replay.isReplayed());
        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
        verify(walletMapper, never()).releaseEscrow(anyLong());
        verify(settlementMapper, never())
                .transitionWaitingToProcessing(anyLong(), anyLong());
    }

    @Test
    void approveLocksWalletsByUserIdWithoutChangingParticipantRoles() {
        Long employerId = 8L;
        Long workerId = 2L;
        WorkCaseEscrowContext context = context("READY");
        context.setEmployerId(employerId);
        context.setWorkerId(workerId);
        SettlementSnapshot waiting = settlement(SettlementStatus.WAITING);
        SettlementSnapshot completed = settlement(SettlementStatus.COMPLETED);
        completed.setApprovedByUserId(employerId);
        WalletTransactionSnapshot held = holdLedger();
        held.setWalletId(80L);
        held.setWalletUserId(employerId);

        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context);
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(waiting, completed);
        when(settlementMapper.transitionWaitingToProcessing(
                SETTLEMENT_ID, employerId)).thenReturn(1);
        when(settlementMapper.transitionProcessingToCompleted(
                SETTLEMENT_ID, employerId)).thenReturn(1);
        when(walletMapper.getWalletSnapshotForUpdate(workerId))
                .thenReturn(wallet(20L, workerId, 0L, 0L));
        when(walletMapper.getWalletSnapshotForUpdate(employerId))
                .thenReturn(wallet(80L, employerId, 400_000L, AGREED_WAGE));
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID))
                .thenReturn("HELD");
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID))
                .thenReturn(AGREED_WAGE);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID))
                .thenReturn(ESCROW_ID);
        when(walletMapper.findEscrowHoldTransactionSnapshot(
                WORK_CASE_ID, ESCROW_ID)).thenReturn(held);
        when(walletMapper.releaseEscrow(WORK_CASE_ID)).thenReturn(1);
        when(walletMapper.releaseLockedFunds(employerId, AGREED_WAGE))
                .thenReturn(1);
        when(walletMapper.addAvailableBalance(workerId, AGREED_WAGE))
                .thenReturn(1);
        when(workMapper.updateWorkStatus(
                WORK_CASE_ID, RELEASABLE, "COMPLETED")).thenReturn(1);
        when(walletMapper.insertWalletTransaction(any())).thenReturn(1);

        settlementService.approve(SettlementApproveCommand.builder()
                .workCaseId(WORK_CASE_ID)
                .approverUserId(employerId)
                .idempotencyKey(KEY)
                .build());

        InOrder walletOrder = inOrder(walletMapper);
        walletOrder.verify(walletMapper).getWalletSnapshotForUpdate(workerId);
        walletOrder.verify(walletMapper).getWalletSnapshotForUpdate(employerId);

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper, times(2)).insertWalletTransaction(captor.capture());
        assertEquals(80L, captor.getAllValues().get(0).getWalletId());
        assertEquals(20L, captor.getAllValues().get(1).getWalletId());
    }

    @Test
    void approveRejectsMissingSettlementBeforeWalletMutation() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));

        assertThrows(
                EscrowIntegrityException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
        verify(walletMapper, never()).releaseEscrow(anyLong());
    }

    @Test
    void approveRejectsSettlementAmountMismatch() {
        SettlementSnapshot settlement = settlement(SettlementStatus.WAITING);
        settlement.setAmount(AGREED_WAGE - 1);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement);

        assertThrows(
                EscrowIntegrityException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(settlementMapper, never())
                .transitionWaitingToProcessing(anyLong(), anyLong());
    }

    @Test
    void approveRejectsOnHoldSettlement() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement(SettlementStatus.ON_HOLD));

        assertThrows(
                InvalidEscrowStateException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    void approveRejectsWorkCaseWithBlockingDispute() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement(SettlementStatus.WAITING));
        when(settlementMapper.findBlockingDisputeIdsForUpdate(WORK_CASE_ID))
                .thenReturn(List.of(9L));

        assertThrows(
                InvalidEscrowStateException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(settlementMapper, never())
                .transitionWaitingToProcessing(anyLong(), anyLong());
        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    void approveRejectsProcessingSettlementAsIntegrityFailure() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement(SettlementStatus.PROCESSING));

        assertThrows(
                EscrowIntegrityException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );
    }

    @Test
    void approveRejectsCompletedSettlementWithoutMatchingReplayLedger() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("COMPLETED"));
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement(SettlementStatus.COMPLETED));

        assertThrows(
                InvalidEscrowStateException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(walletMapper, never()).releaseEscrow(anyLong());
    }

    @Test
    void approveRejectsIncompleteReplayPair() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("COMPLETED"));
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement(SettlementStatus.COMPLETED));
        when(settlementLedgerMapper.findByIdempotencyKeyForShare(
                WalletIdempotencyKeys.escrowReleaseEmployer(KEY)))
                .thenReturn(releaseLedger(EMPLOYER_ID));

        assertThrows(
                EscrowIntegrityException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(walletMapper, never()).releaseEscrow(anyLong());
    }

    @Test
    void approveRejectsUnauthorizedApproverBeforeSettlementLock() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));

        assertThrows(
                EscrowAccessDeniedException.class,
                () -> settlementService.approve(command(99L))
        );

        verify(settlementMapper, never()).findByWorkCaseIdForUpdate(anyLong());
    }

    @Test
    void approveRejectsSameEmployerAndWorker() {
        WorkCaseEscrowContext invalid = context("ACCEPTED");
        invalid.setWorkerId(EMPLOYER_ID);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(invalid);

        assertThrows(
                InvalidEscrowStateException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(settlementMapper, never()).findByWorkCaseIdForUpdate(anyLong());
    }

    @Test
    void approveRejectsChangedEmployerAgainstHeldLedgerOwner() {
        Long changedEmployerId = 8L;
        WorkCaseEscrowContext context = context("ACCEPTED");
        context.setEmployerId(changedEmployerId);
        SettlementSnapshot waiting = settlement(SettlementStatus.WAITING);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context);
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(waiting);
        when(settlementMapper.transitionWaitingToProcessing(
                SETTLEMENT_ID, changedEmployerId)).thenReturn(1);
        when(walletMapper.getWalletSnapshotForUpdate(WORKER_ID))
                .thenReturn(wallet(40L, WORKER_ID, 0L, 0L));
        when(walletMapper.getWalletSnapshotForUpdate(changedEmployerId))
                .thenReturn(wallet(
                        80L,
                        changedEmployerId,
                        0L,
                        AGREED_WAGE
                ));
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID))
                .thenReturn("HELD");
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID))
                .thenReturn(AGREED_WAGE);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID))
                .thenReturn(ESCROW_ID);
        when(walletMapper.findEscrowHoldTransactionSnapshot(
                WORK_CASE_ID, ESCROW_ID)).thenReturn(holdLedger());

        assertThrows(
                EscrowIntegrityException.class,
                () -> settlementService.approve(
                        command(changedEmployerId)
                )
        );

        verify(walletMapper, never()).releaseEscrow(anyLong());
        verify(walletMapper, never())
                .releaseLockedFunds(anyLong(), anyLong());
    }

    @Test
    void approveTranslatesConcurrentReleaseLedgerCollision() {
        stubNewApproval(context("ACCEPTED"));
        when(walletMapper.insertWalletTransaction(any()))
                .thenReturn(1)
                .thenThrow(new DuplicateKeyException("concurrent ledger"));

        assertThrows(
                IdempotencyKeyReusedException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(walletMapper, times(2)).insertWalletTransaction(any());
    }

    @Test
    void approveRejectsFailedCompletedTransitionAfterPayoutWrites() {
        stubNewApproval(context("ACCEPTED"));
        when(settlementMapper.transitionProcessingToCompleted(
                SETTLEMENT_ID, EMPLOYER_ID)).thenReturn(0);

        assertThrows(
                EscrowIntegrityException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(walletMapper).releaseEscrow(WORK_CASE_ID);
        verify(walletMapper).releaseLockedFunds(EMPLOYER_ID, AGREED_WAGE);
        verify(walletMapper).addAvailableBalance(WORKER_ID, AGREED_WAGE);
        verify(walletMapper, times(2)).insertWalletTransaction(any());
    }

    @Test
    void approveAcceptsAlreadyCompletedWorkCaseWithoutRewritingIt() {
        stubNewApproval(context("COMPLETED"));

        SettlementResult result =
                settlementService.approve(command(EMPLOYER_ID));

        assertEquals("COMPLETED", result.getStatus());
        verify(workMapper, never()).updateWorkStatus(
                anyLong(),
                any(),
                any()
        );
    }

    @Test
    void approveStopsWhenProcessingTransitionAffectsNoRow() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(settlement(SettlementStatus.WAITING));

        assertThrows(
                EscrowIntegrityException.class,
                () -> settlementService.approve(command(EMPLOYER_ID))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    private void stubNewApproval(WorkCaseEscrowContext context) {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context);
        when(settlementMapper.findByWorkCaseIdForUpdate(WORK_CASE_ID))
                .thenReturn(
                        settlement(SettlementStatus.WAITING),
                        settlement(SettlementStatus.COMPLETED)
                );
        when(settlementMapper.transitionWaitingToProcessing(
                SETTLEMENT_ID, EMPLOYER_ID)).thenReturn(1);
        lenient().when(settlementMapper.transitionProcessingToCompleted(
                SETTLEMENT_ID, EMPLOYER_ID)).thenReturn(1);
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID))
                .thenReturn(wallet(30L, EMPLOYER_ID, 400_000L, AGREED_WAGE));
        when(walletMapper.getWalletSnapshotForUpdate(WORKER_ID))
                .thenReturn(wallet(40L, WORKER_ID, 0L, 0L));
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID))
                .thenReturn("HELD");
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID))
                .thenReturn(AGREED_WAGE);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID))
                .thenReturn(ESCROW_ID);
        when(walletMapper.findEscrowHoldTransactionSnapshot(
                WORK_CASE_ID, ESCROW_ID))
                .thenReturn(holdLedger());
        when(walletMapper.releaseEscrow(WORK_CASE_ID)).thenReturn(1);
        when(walletMapper.releaseLockedFunds(EMPLOYER_ID, AGREED_WAGE))
                .thenReturn(1);
        when(walletMapper.addAvailableBalance(WORKER_ID, AGREED_WAGE))
                .thenReturn(1);
        if (!"COMPLETED".equals(context.getStatus())) {
            when(workMapper.updateWorkStatus(
                    WORK_CASE_ID, RELEASABLE, "COMPLETED"))
                    .thenReturn(1);
        }
        when(walletMapper.insertWalletTransaction(any())).thenReturn(1);
    }

    private SettlementApproveCommand command(Long approverId) {
        return SettlementApproveCommand.builder()
                .workCaseId(WORK_CASE_ID)
                .approverUserId(approverId)
                .idempotencyKey(KEY)
                .build();
    }

    private WorkCaseEscrowContext context(String status) {
        WorkCaseEscrowContext context = new WorkCaseEscrowContext();
        context.setWorkCaseId(WORK_CASE_ID);
        context.setEmployerId(EMPLOYER_ID);
        context.setWorkerId(WORKER_ID);
        context.setAgreedWage(AGREED_WAGE);
        context.setStatus(status);
        return context;
    }

    private SettlementSnapshot settlement(SettlementStatus status) {
        SettlementSnapshot settlement = new SettlementSnapshot();
        settlement.setSettlementId(SETTLEMENT_ID);
        settlement.setWorkCaseId(WORK_CASE_ID);
        settlement.setAmount(AGREED_WAGE);
        settlement.setStatus(status);
        if (SettlementStatus.COMPLETED == status) {
            settlement.setApprovedByUserId(EMPLOYER_ID);
            settlement.setProcessingAt(PROCESSING_AT);
            settlement.setCompletedAt(COMPLETED_AT);
        }
        return settlement;
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

    private WalletTransactionSnapshot holdLedger() {
        WalletTransactionSnapshot ledger = new WalletTransactionSnapshot();
        ledger.setId(1L);
        ledger.setWalletId(30L);
        ledger.setWalletUserId(EMPLOYER_ID);
        ledger.setWorkCaseId(WORK_CASE_ID);
        ledger.setTransactionType("ESCROW_HOLD");
        ledger.setAmount(AGREED_WAGE);
        ledger.setAvailableBefore(700_000L);
        ledger.setAvailableAfter(400_000L);
        ledger.setLockedBefore(0L);
        ledger.setLockedAfter(AGREED_WAGE);
        ledger.setReferenceType("ESCROW");
        ledger.setReferenceId(ESCROW_ID);
        return ledger;
    }

    private WalletTransactionSnapshot releaseLedger(Long walletUserId) {
        WalletTransactionSnapshot ledger = new WalletTransactionSnapshot();
        ledger.setId(2L);
        ledger.setWalletId(EMPLOYER_ID.equals(walletUserId) ? 30L : 40L);
        ledger.setWalletUserId(walletUserId);
        ledger.setWorkCaseId(WORK_CASE_ID);
        ledger.setTransactionType("ESCROW_RELEASE");
        ledger.setAmount(AGREED_WAGE);
        if (EMPLOYER_ID.equals(walletUserId)) {
            ledger.setAvailableBefore(400_000L);
            ledger.setAvailableAfter(400_000L);
            ledger.setLockedBefore(AGREED_WAGE);
            ledger.setLockedAfter(0L);
        } else {
            ledger.setAvailableBefore(0L);
            ledger.setAvailableAfter(AGREED_WAGE);
            ledger.setLockedBefore(0L);
            ledger.setLockedAfter(0L);
        }
        ledger.setReferenceType("ESCROW");
        ledger.setReferenceId(ESCROW_ID);
        return ledger;
    }
}
