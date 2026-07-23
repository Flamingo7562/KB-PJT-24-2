package com.gighub.wallet.service;

import com.gighub.wallet.exception.InsufficientWalletBalanceException;
import com.gighub.wallet.exception.InvalidEscrowStateException;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.wallet.mapper.param.WalletTransactionParam;
import com.gighub.wallet.service.command.EscrowHoldCommand;
import com.gighub.wallet.service.command.EscrowReleaseCommand;
import com.gighub.wallet.service.impl.EscrowServiceImpl;
import com.gighub.work.mapper.WorkMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
    private static final List<String> RELEASABLE = List.of("ACCEPTED", "READY", "IN_PROGRESS");

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WorkMapper workMapper;

    // 인터페이스가 아니라 구현체를 주입한다.
    @InjectMocks
    private EscrowServiceImpl escrowService;

    private EscrowHoldCommand holdCommand(Long amount) {
        return EscrowHoldCommand.builder()
                .employerId(EMPLOYER_ID)
                .workerId(WORKER_ID)
                .workCaseId(WORK_CASE_ID)
                .amount(amount)
                .idempotencyKey(KEY)
                .build();
    }

    private EscrowReleaseCommand releaseCommand() {
        return EscrowReleaseCommand.builder()
                .workCaseId(WORK_CASE_ID)
                .employerId(EMPLOYER_ID)
                .idempotencyKey(KEY)
                .build();
    }

    // ===================== 예치(hold) =====================

    @Test
    @DisplayName("T-07: 정상 예치 시 지갑 잠금, 에스크로 생성, 근무 상태 전이, 원장 기록이 각 1회 수행된다")
    void hold_success() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY)).thenReturn(0);
        when(workMapper.getAgreedWageByWorkCaseId(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(workMapper.getWorkerIdByWorkCaseId(WORK_CASE_ID)).thenReturn(WORKER_ID);
        when(walletMapper.getAvailableBalanceForUpdate(EMPLOYER_ID)).thenReturn(700_000L);
        when(walletMapper.getLockedBalance(EMPLOYER_ID)).thenReturn(0L);
        when(walletMapper.getWalletIdByUserId(EMPLOYER_ID)).thenReturn(30L);
        when(walletMapper.lockEmployerFunds(EMPLOYER_ID, AGREED_WAGE)).thenReturn(1);
        // 선행 escrow 행이 없는 경우 -> INSERT 경로
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID)).thenReturn(null);
        when(workMapper.updateWorkStatus(WORK_CASE_ID, HOLDABLE, "ACCEPTED")).thenReturn(1);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertDoesNotThrow(() -> escrowService.hold(holdCommand(AGREED_WAGE)));

        verify(walletMapper, times(1)).lockEmployerFunds(EMPLOYER_ID, AGREED_WAGE);
        verify(walletMapper, times(1)).insertEscrowRecord(WORK_CASE_ID, AGREED_WAGE);
        verify(walletMapper, never()).holdEscrow(anyLong());
        verify(workMapper, times(1)).updateWorkStatus(WORK_CASE_ID, HOLDABLE, "ACCEPTED");

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper, times(1)).insertWalletTransaction(captor.capture());

        WalletTransactionParam ledger = captor.getValue();
        assertEquals(30L, ledger.getWalletId());
        assertEquals("ESCROW_HOLD", ledger.getTransactionType());
        assertEquals(AGREED_WAGE, ledger.getAmount());
        // 예치: available 감소, locked 증가
        assertEquals(700_000L, ledger.getAvailableBefore());
        assertEquals(400_000L, ledger.getAvailableAfter());
        assertEquals(0L, ledger.getLockedBefore());
        assertEquals(300_000L, ledger.getLockedAfter());
        assertEquals("ESCROW", ledger.getReferenceType());
        assertEquals(11L, ledger.getReferenceId());
        assertEquals(KEY, ledger.getIdempotencyKey());
    }

    @Test
    @DisplayName("T-07-b: 선행 escrow가 UNFUNDED면 INSERT 대신 HELD로 전이한다")
    void hold_transitionsUnfundedEscrow() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY)).thenReturn(0);
        when(workMapper.getAgreedWageByWorkCaseId(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(workMapper.getWorkerIdByWorkCaseId(WORK_CASE_ID)).thenReturn(WORKER_ID);
        when(walletMapper.getAvailableBalanceForUpdate(EMPLOYER_ID)).thenReturn(700_000L);
        when(walletMapper.getLockedBalance(EMPLOYER_ID)).thenReturn(0L);
        when(walletMapper.getWalletIdByUserId(EMPLOYER_ID)).thenReturn(30L);
        when(walletMapper.lockEmployerFunds(EMPLOYER_ID, AGREED_WAGE)).thenReturn(1);
        when(walletMapper.getEscrowStatusForUpdate(WORK_CASE_ID)).thenReturn("UNFUNDED");
        when(walletMapper.holdEscrow(WORK_CASE_ID)).thenReturn(1);
        when(workMapper.updateWorkStatus(WORK_CASE_ID, HOLDABLE, "ACCEPTED")).thenReturn(1);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);

        assertDoesNotThrow(() -> escrowService.hold(holdCommand(AGREED_WAGE)));

        verify(walletMapper, times(1)).holdEscrow(WORK_CASE_ID);
        verify(walletMapper, never()).insertEscrowRecord(anyLong(), anyLong());
    }

    @Test
    @DisplayName("T-06: 잔액 부족 시 예외가 발생하고 어떤 변경도 수행되지 않는다")
    void hold_fail_insufficientBalance() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY)).thenReturn(0);
        when(workMapper.getAgreedWageByWorkCaseId(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(workMapper.getWorkerIdByWorkCaseId(WORK_CASE_ID)).thenReturn(WORKER_ID);
        when(walletMapper.getAvailableBalanceForUpdate(EMPLOYER_ID)).thenReturn(30_000L);

        InsufficientWalletBalanceException e = assertThrows(
                InsufficientWalletBalanceException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE)));

        assertTrue(e.getMessage().contains("지갑 잔액이 부족하여"));

        verify(walletMapper, never()).lockEmployerFunds(anyLong(), anyLong());
        verify(walletMapper, never()).insertEscrowRecord(anyLong(), anyLong());
        verify(workMapper, never()).updateWorkStatus(anyLong(), any(), anyString());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    @DisplayName("T-08: 이미 처리된 멱등 키면 아무 작업 없이 조기 종료한다")
    void hold_skipsWhenAlreadyProcessed() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY)).thenReturn(1);

        assertDoesNotThrow(() -> escrowService.hold(holdCommand(AGREED_WAGE)));

        verify(workMapper, never()).getAgreedWageByWorkCaseId(anyLong());
        verify(walletMapper, never()).getAvailableBalanceForUpdate(anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    @DisplayName("T-09: 요청 금액이 약정 임금과 다르면 예치를 거부한다")
    void hold_fail_amountMismatch() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY)).thenReturn(0);
        when(workMapper.getAgreedWageByWorkCaseId(WORK_CASE_ID)).thenReturn(AGREED_WAGE);

        assertThrows(InvalidEscrowStateException.class,
                () -> escrowService.hold(holdCommand(299_999L)));

        verify(walletMapper, never()).lockEmployerFunds(anyLong(), anyLong());
    }

    @Test
    @DisplayName("T-10: 근무 건에 배정된 알바생이 아니면 예치를 거부한다")
    void hold_fail_workerMismatch() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY)).thenReturn(0);
        when(workMapper.getAgreedWageByWorkCaseId(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(workMapper.getWorkerIdByWorkCaseId(WORK_CASE_ID)).thenReturn(99L);

        assertThrows(InvalidEscrowStateException.class,
                () -> escrowService.hold(holdCommand(AGREED_WAGE)));

        verify(walletMapper, never()).lockEmployerFunds(anyLong(), anyLong());
    }

    // ===================== 정산(release) =====================

    @Test
    @DisplayName("T-11: 정산 시 고용주 locked 감소, 알바생 available 증가, 원장 2건이 기록된다")
    void release_success() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY + "_OUT")).thenReturn(0);
        when(workMapper.getWorkerIdByWorkCaseId(WORK_CASE_ID)).thenReturn(WORKER_ID);
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(walletMapper.getAvailableBalance(EMPLOYER_ID)).thenReturn(400_000L);
        when(walletMapper.getLockedBalance(EMPLOYER_ID)).thenReturn(300_000L);
        when(walletMapper.getWalletIdByUserId(EMPLOYER_ID)).thenReturn(30L);
        when(walletMapper.getAvailableBalance(WORKER_ID)).thenReturn(0L);
        when(walletMapper.getLockedBalance(WORKER_ID)).thenReturn(0L);
        when(walletMapper.getWalletIdByUserId(WORKER_ID)).thenReturn(40L);
        when(walletMapper.getEscrowIdByWorkCaseId(WORK_CASE_ID)).thenReturn(11L);
        when(walletMapper.releaseEscrow(WORK_CASE_ID)).thenReturn(1);
        when(walletMapper.releaseLockedFunds(EMPLOYER_ID, AGREED_WAGE)).thenReturn(1);
        when(walletMapper.addAvailableBalance(WORKER_ID, AGREED_WAGE)).thenReturn(1);
        when(workMapper.updateWorkStatus(WORK_CASE_ID, RELEASABLE, "COMPLETED")).thenReturn(1);

        assertDoesNotThrow(() -> escrowService.release(releaseCommand()));

        ArgumentCaptor<WalletTransactionParam> captor =
                ArgumentCaptor.forClass(WalletTransactionParam.class);
        verify(walletMapper, times(2)).insertWalletTransaction(captor.capture());

        WalletTransactionParam out = captor.getAllValues().get(0);
        assertEquals(30L, out.getWalletId());
        assertEquals(KEY + "_OUT", out.getIdempotencyKey());
        // 고용주: available 불변, locked만 감소
        assertEquals(400_000L, out.getAvailableBefore());
        assertEquals(400_000L, out.getAvailableAfter());
        assertEquals(300_000L, out.getLockedBefore());
        assertEquals(0L, out.getLockedAfter());

        WalletTransactionParam in = captor.getAllValues().get(1);
        assertEquals(40L, in.getWalletId());
        assertEquals(KEY + "_IN", in.getIdempotencyKey());
        // 알바생: available만 증가, locked 불변
        assertEquals(0L, in.getAvailableBefore());
        assertEquals(300_000L, in.getAvailableAfter());
        assertEquals(0L, in.getLockedBefore());
        assertEquals(0L, in.getLockedAfter());
    }

    @Test
    @DisplayName("T-12: HELD 상태 에스크로가 없으면 정산을 거부한다")
    void release_fail_notHeld() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY + "_OUT")).thenReturn(0);
        when(workMapper.getWorkerIdByWorkCaseId(WORK_CASE_ID)).thenReturn(WORKER_ID);
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID)).thenReturn(null);

        assertThrows(InvalidEscrowStateException.class,
                () -> escrowService.release(releaseCommand()));

        verify(walletMapper, never()).releaseLockedFunds(anyLong(), anyLong());
        verify(walletMapper, never()).addAvailableBalance(anyLong(), anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }

    @Test
    @DisplayName("T-13: 멱등 키가 달라도 이미 RELEASED면 이중 정산이 차단된다")
    void release_fail_alreadyReleased() {
        when(walletMapper.countTransactionByIdempotencyKey(KEY + "_OUT")).thenReturn(0);
        when(workMapper.getWorkerIdByWorkCaseId(WORK_CASE_ID)).thenReturn(WORKER_ID);
        when(walletMapper.getHeldEscrowAmount(WORK_CASE_ID)).thenReturn(AGREED_WAGE);
        when(walletMapper.getAvailableBalance(EMPLOYER_ID)).thenReturn(400_000L);
        when(walletMapper.getLockedBalance(EMPLOYER_ID)).thenReturn(300_000L);
        when(walletMapper.getWalletIdByUserId(EMPLOYER_ID)).thenReturn(30L);
        when(walletMapper.getAvailableBalance(WORKER_ID)).thenReturn(0L);
        when(walletMapper.getLockedBalance(WORKER_ID)).thenReturn(0L);
        when(walletMapper.getWalletIdByUserId(WORKER_ID)).thenReturn(40L);
        // HELD -> RELEASED 전이 실패 = 이미 정산된 건
        when(walletMapper.releaseEscrow(WORK_CASE_ID)).thenReturn(0);

        assertThrows(InvalidEscrowStateException.class,
                () -> escrowService.release(releaseCommand()));

        verify(walletMapper, never()).releaseLockedFunds(anyLong(), anyLong());
        verify(walletMapper, never()).addAvailableBalance(anyLong(), anyLong());
        verify(walletMapper, never()).insertWalletTransaction(any());
    }
}