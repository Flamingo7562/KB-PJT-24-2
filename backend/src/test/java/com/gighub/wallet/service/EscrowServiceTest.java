package com.gighub.wallet.service;

import com.gighub.wallet.dto.WalletBalanceSnapshot;
import com.gighub.wallet.dto.WalletTransactionSnapshot;
import com.gighub.wallet.exception.EscrowAccessDeniedException;
import com.gighub.wallet.exception.EscrowIntegrityException;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InsufficientWalletBalanceException;
import com.gighub.wallet.idempotency.WalletIdempotencyKeys;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.wallet.service.command.EscrowHoldCommand;
import com.gighub.wallet.service.impl.EscrowServiceImpl;
import com.gighub.work.dto.WorkCaseEscrowContext;
import com.gighub.work.mapper.WorkMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
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

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WorkMapper workMapper;

    @InjectMocks
    private EscrowServiceImpl escrowService;

    @Test
    void holdUsesLockedWalletSnapshotAndRecordsLedger() {
        WorkCaseEscrowContext context = context("INVITED");
        WalletBalanceSnapshot wallet =
                wallet(30L, EMPLOYER_ID, 700_000L, 0L);

        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID)).thenReturn(context);
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID)).thenReturn(wallet);
        when(walletMapper.lockEmployerFunds(EMPLOYER_ID, AGREED_WAGE)).thenReturn(1);
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID)).thenReturn(null);
        when(walletMapper.insertEscrowRecord(WORK_CASE_ID, AGREED_WAGE)).thenReturn(1);
        when(workMapper.updateWorkStatus(WORK_CASE_ID, HOLDABLE, "ACCEPTED"))
                .thenReturn(1);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);
        when(walletMapper.insertWalletTransaction(any())).thenReturn(1);

        assertDoesNotThrow(
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper).insertWalletTransaction(captor.capture());
        WalletTransactionParam transaction = captor.getValue();
        assertEquals(400_000L, transaction.getAvailableAfter());
        assertEquals(300_000L, transaction.getLockedAfter());
        assertEquals(
                WalletIdempotencyKeys.escrowHold(KEY),
                transaction.getIdempotencyKey()
        );

        InOrder lockOrder = inOrder(workMapper, walletMapper);
        lockOrder.verify(workMapper).getEscrowContextForUpdate(WORK_CASE_ID);
        lockOrder.verify(walletMapper).getWalletSnapshotForUpdate(EMPLOYER_ID);
        lockOrder.verify(walletMapper).getEscrowStatusForUpdate(WORK_CASE_ID);
    }

    @Test
    void holdRejectsInsufficientBalanceBeforeMutation() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("INVITED"));
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID))
                .thenReturn(wallet(30L, EMPLOYER_ID, 30_000L, 0L));

        assertThrows(
                InsufficientWalletBalanceException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).lockEmployerFunds(anyLong(), anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    void holdRejectsCorruptedWalletSnapshot() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("INVITED"));
        WalletBalanceSnapshot corrupted =
                wallet(30L, WORKER_ID, 700_000L, 0L);
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID))
                .thenReturn(corrupted);

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).lockEmployerFunds(anyLong(), anyLong());
    }

    @Test
    void holdRejectsUnexpectedWalletUpdateCount() {
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("INVITED"));
        when(walletMapper.getWalletSnapshotForUpdate(EMPLOYER_ID))
                .thenReturn(wallet(30L, EMPLOYER_ID, 700_000L, 0L));

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).insertEscrowRecord(anyLong(), anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    void holdRejectsEmployerMismatch() {
        WorkCaseEscrowContext context = context("INVITED");
        context.setEmployerId(99L);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context);

        assertThrows(
                EscrowAccessDeniedException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    void holdReplaysMatchingLedgerWithoutMutation() {
        WorkCaseEscrowContext context = context("ACCEPTED");
        String ledgerKey = WalletIdempotencyKeys.escrowHold(KEY);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context);
        when(walletMapper.findTransactionByIdempotencyKey(ledgerKey))
                .thenReturn(holdTransaction(WORK_CASE_ID));
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertDoesNotThrow(
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    void holdRejectsInvalidReplayBalanceSnapshot() {
        String ledgerKey = WalletIdempotencyKeys.escrowHold(KEY);
        WalletTransactionSnapshot replay = holdTransaction(WORK_CASE_ID);
        replay.setLockedAfter(AGREED_WAGE - 1);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));
        when(walletMapper.findTransactionByIdempotencyKey(ledgerKey))
                .thenReturn(replay);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );

        verify(walletMapper, never()).getWalletSnapshotForUpdate(anyLong());
    }

    @Test
    void holdRejectsReplayWithDifferentEscrowReference() {
        String ledgerKey = WalletIdempotencyKeys.escrowHold(KEY);
        WalletTransactionSnapshot replay = holdTransaction(WORK_CASE_ID);
        replay.setReferenceId(99L);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("ACCEPTED"));
        when(walletMapper.findTransactionByIdempotencyKey(ledgerKey))
                .thenReturn(replay);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertThrows(
                EscrowIntegrityException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );
    }

    @Test
    void holdRejectsReplayForDifferentWorkCase() {
        String ledgerKey = WalletIdempotencyKeys.escrowHold(KEY);
        when(workMapper.getEscrowContextForUpdate(WORK_CASE_ID))
                .thenReturn(context("INVITED"));
        when(walletMapper.findTransactionByIdempotencyKey(ledgerKey))
                .thenReturn(holdTransaction(99L));

        assertThrows(
                IdempotencyKeyReusedException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE))
        );
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

    private WorkCaseEscrowContext context(String status) {
        WorkCaseEscrowContext context = new WorkCaseEscrowContext();
        context.setWorkCaseId(WORK_CASE_ID);
        context.setEmployerId(EMPLOYER_ID);
        context.setWorkerId(WORKER_ID);
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

    private WalletTransactionSnapshot holdTransaction(Long workCaseId) {
        WalletTransactionSnapshot transaction = new WalletTransactionSnapshot();
        transaction.setId(1L);
        transaction.setWalletId(30L);
        transaction.setWalletUserId(EMPLOYER_ID);
        transaction.setWorkCaseId(workCaseId);
        transaction.setTransactionType("ESCROW_HOLD");
        transaction.setAmount(AGREED_WAGE);
        transaction.setAvailableBefore(700_000L);
        transaction.setAvailableAfter(400_000L);
        transaction.setLockedBefore(0L);
        transaction.setLockedAfter(300_000L);
        transaction.setReferenceType("ESCROW");
        transaction.setReferenceId(11L);
        return transaction;
    }
}
