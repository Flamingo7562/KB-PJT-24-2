package com.gighub.bank.service;

import com.gighub.bank.dto.MockBankAccount;
import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.exception.BankTransferIntegrityException;
import com.gighub.bank.exception.InsufficientBankBalanceException;
import com.gighub.bank.mapper.MockBankMapper;
import com.gighub.bank.mapper.param.BankTransactionParam;
import com.gighub.bank.service.impl.MockBankTransferGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockBankTransferGatewayTest {

    private static final Long ACCOUNT_ID = 10L;
    private static final Long USER_ID = 3L;
    private static final Long AMOUNT = 300_000L;

    @Mock
    private MockBankMapper mockBankMapper;

    @InjectMocks
    private MockBankTransferGateway gateway;

    @Test
    @DisplayName("게이트웨이의 모든 작업은 기존 자금 트랜잭션을 필수로 요구한다")
    void gatewayRequiresExistingTransaction() {
        Transactional annotation =
                MockBankTransferGateway.class.getAnnotation(Transactional.class);

        assertEquals(Propagation.MANDATORY, annotation.propagation());
    }

    @Test
    @DisplayName("사전 검증은 계좌 소유권을 구현체 안에서 확인한다")
    void preflightRejectsAnotherUsersAccount() {
        MockBankAccount account = account(USER_ID + 1, "ACTIVE", 1_000_000L);
        when(mockBankMapper.getAccountById(ACCOUNT_ID)).thenReturn(account);

        assertThrows(
                BankAccountForbiddenException.class,
                () -> gateway.preflight(preflight())
        );
    }

    @Test
    @DisplayName("출금은 계좌를 다시 잠그고 성공 은행 원장 결과를 반환한다")
    void withdrawLocksAccountAndReturnsVerifiedResult() {
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID))
                .thenReturn(account(USER_ID, "ACTIVE", 1_000_000L));
        when(mockBankMapper.withdrawFromAccount(ACCOUNT_ID, AMOUNT)).thenReturn(1);
        when(mockBankMapper.insertBankTransaction(any())).thenAnswer(invocation -> {
            BankTransactionParam param = invocation.getArgument(0);
            param.setId(31L);
            return 1;
        });

        BankTransferResult result = gateway.withdraw(transferCommand());

        assertEquals(31L, result.getBankTransactionId());
        assertEquals("SUCCESS", result.getStatus());
        assertEquals(AMOUNT, result.getTransferredAmount());
        assertEquals(1_000_000L, result.getBalanceBefore());
        assertEquals(700_000L, result.getBalanceAfter());
        assertTrue(result.getBankTranId().startsWith("M"));

        ArgumentCaptor<BankTransactionParam> captor =
                ArgumentCaptor.forClass(BankTransactionParam.class);
        verify(mockBankMapper).insertBankTransaction(captor.capture());
        assertEquals("WITHDRAW", captor.getValue().getTransferType());
        assertEquals("FUNDING_ORDER", captor.getValue().getReferenceType());
        assertEquals(21L, captor.getValue().getReferenceId());
    }

    @Test
    @DisplayName("잠금 후 계좌 잔액이 부족하면 계좌와 원장을 변경하지 않는다")
    void withdrawRejectsInsufficientBalance() {
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID))
                .thenReturn(account(USER_ID, "ACTIVE", AMOUNT - 1));

        assertThrows(
                InsufficientBankBalanceException.class,
                () -> gateway.withdraw(transferCommand())
        );

        verify(mockBankMapper, never()).withdrawFromAccount(ACCOUNT_ID, AMOUNT);
        verify(mockBankMapper, never()).insertBankTransaction(any());
    }

    @Test
    @DisplayName("은행 원장 INSERT 결과나 생성 ID가 없으면 성공 결과를 반환하지 않는다")
    void withdrawRejectsMissingLedgerIdentity() {
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID))
                .thenReturn(account(USER_ID, "ACTIVE", 1_000_000L));
        when(mockBankMapper.withdrawFromAccount(ACCOUNT_ID, AMOUNT)).thenReturn(1);
        when(mockBankMapper.insertBankTransaction(any())).thenReturn(1);

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.withdraw(transferCommand())
        );
    }

    @Test
    @DisplayName("0 이하 이체 금액은 계좌 잠금 전에 게이트웨이 계약 오류로 거부한다")
    void withdrawRejectsInvalidCommandBeforeLock() {
        BankTransferCommand command = BankTransferCommand.builder()
                .accountId(ACCOUNT_ID)
                .userId(USER_ID)
                .amount(0L)
                .referenceType("FUNDING_ORDER")
                .referenceId(21L)
                .build();

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.withdraw(command)
        );

        verifyNoInteractions(mockBankMapper);
    }

    @Test
    @DisplayName("조회된 계좌 식별자가 비정상이면 서버 무결성 오류를 반환한다")
    void preflightRejectsMalformedAccountIdentity() {
        MockBankAccount account = account(USER_ID, "ACTIVE", 1_000_000L);
        account.setId(null);
        when(mockBankMapper.getAccountById(ACCOUNT_ID)).thenReturn(account);

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.preflight(preflight())
        );
    }

    @Test
    @DisplayName("잠금 계좌 UPDATE가 0건이면 잔액 부족이 아닌 서버 무결성 오류다")
    void withdrawRejectsUnexpectedUpdateCount() {
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID))
                .thenReturn(account(USER_ID, "ACTIVE", 1_000_000L));
        when(mockBankMapper.withdrawFromAccount(ACCOUNT_ID, AMOUNT)).thenReturn(0);

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.withdraw(transferCommand())
        );

        verify(mockBankMapper, never()).insertBankTransaction(any());
    }

    @Test
    @DisplayName("스키마 제약과 맞지 않는 계좌 잔액은 서버 무결성 오류다")
    void withdrawRejectsCorruptedAccountBalance() {
        MockBankAccount account = account(USER_ID, "ACTIVE", 1_000_000L);
        account.setAvailableAmount(1_000_001L);
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID)).thenReturn(account);

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.withdraw(transferCommand())
        );

        verify(mockBankMapper, never()).withdrawFromAccount(ACCOUNT_ID, AMOUNT);
    }

    @Test
    @DisplayName("입금 후 잔액이 long 범위를 넘으면 계좌를 변경하지 않는다")
    void depositRejectsBalanceOverflow() {
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID))
                .thenReturn(account(USER_ID, "ACTIVE", Long.MAX_VALUE));

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.deposit(transferCommand())
        );

        verify(mockBankMapper, never()).depositToAccount(ACCOUNT_ID, AMOUNT);
        verify(mockBankMapper, never()).insertBankTransaction(any());
    }

    private BankAccountPreflightCommand preflight() {
        return BankAccountPreflightCommand.builder()
                .accountId(ACCOUNT_ID)
                .userId(USER_ID)
                .build();
    }

    private BankTransferCommand transferCommand() {
        return BankTransferCommand.builder()
                .accountId(ACCOUNT_ID)
                .userId(USER_ID)
                .amount(AMOUNT)
                .referenceType("FUNDING_ORDER")
                .referenceId(21L)
                .build();
    }

    private MockBankAccount account(Long userId, String status, Long amount) {
        MockBankAccount account = new MockBankAccount();
        account.setId(ACCOUNT_ID);
        account.setUserId(userId);
        account.setBalance(amount);
        account.setAvailableAmount(amount);
        account.setStatus(status);
        return account;
    }
}
