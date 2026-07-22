package com.gighub.wallet.service;

import com.gighub.wallet.dto.EscrowLockRequest;
import com.gighub.wallet.mapper.WalletMapper;
import com.gighub.work.mapper.WorkMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EscrowServiceTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WorkMapper workMapper;

    @InjectMocks
    private EscrowService escrowService;

    @Test
    @DisplayName("T-07: 정상적인 예치(Lock) 성공 테스트")
    void lockFunds_Success() {
        // given (테스트 준비)
        EscrowLockRequest request = new EscrowLockRequest();
        request.setEmployerId(1L);
        request.setWorkCaseId(100L);
        request.setAmount(new BigDecimal("50000"));

        // 사장님의 지갑에 10만 원이 있다고 가정 (가짜 응답 설정)
        when(walletMapper.getAvailableBalance(1L)).thenReturn(new BigDecimal("100000"));
        // DB 업데이트가 정상적으로 1건씩 처리되었다고 가정
        when(walletMapper.lockEmployerFunds(eq(1L), any(BigDecimal.class))).thenReturn(1);
        when(walletMapper.insertEscrowRecord(eq(100L), any(BigDecimal.class), eq("HELD"))).thenReturn(1);
        when(workMapper.updateWorkStatus(100L, "HELD")).thenReturn(1);

        // when (실행)
        assertDoesNotThrow(() -> escrowService.lockFunds(request));

        // then (검증)
        // 매퍼의 해당 메서드들이 정확히 1번씩 호출되었는지 확인
        verify(walletMapper, times(1)).lockEmployerFunds(1L, new BigDecimal("50000"));
        verify(walletMapper, times(1)).insertEscrowRecord(100L, new BigDecimal("50000"), "HELD");
        verify(workMapper, times(1)).updateWorkStatus(100L, "HELD");
    }

    @Test
    @DisplayName("T-06: 지갑 잔액 부족 시 예치(Lock) 실패 및 예외 발생 테스트")
    void lockFunds_Fail_InsufficientBalance() {
        // given (테스트 준비)
        EscrowLockRequest request = new EscrowLockRequest();
        request.setEmployerId(1L);
        request.setAmount(new BigDecimal("50000")); // 5만 원 예치 시도

        // 사장님의 지갑에 3만 원밖에 없다고 가정
        when(walletMapper.getAvailableBalance(1L)).thenReturn(new BigDecimal("30000"));

        // when & then (실행 및 검증)
        // 잔액 부족 예외가 터져야 성공하는 테스트
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            escrowService.lockFunds(request);
        });

        assertTrue(exception.getMessage().contains("지갑 잔액이 부족하여"));

        // 잔액이 부족했으므로 아래 업데이트 로직들은 단 한 번도 실행되지 않아야 함 (안전성 검증)
        verify(walletMapper, never()).lockEmployerFunds(anyLong(), any());
        verify(walletMapper, never()).insertEscrowRecord(anyLong(), any(), any());
        verify(workMapper, never()).updateWorkStatus(anyLong(), any());
    }
}
