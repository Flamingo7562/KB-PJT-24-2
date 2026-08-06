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
    private static final String BANK_CODE = "004";
    private static final String ACCOUNT_NO = "1234567890";
    private static final String PIN = "0000";
    private static final Long AMOUNT = 300_000L;

    @Mock
    private MockBankMapper mockBankMapper;

    @InjectMocks
    private MockBankTransferGateway gateway;

    @Test
    @DisplayName("계좌 잠금은 상태·PIN을 검사하지 않고 X-lock만 선점한다")
    void lockAccountTakesExclusiveLockWithoutStateCheck() {
        // 상태가 BLOCKED여도 잠금 단계에서는 통과한다. 검증은 claim 선점 이후 preflight의 책임이다.
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID))
                .thenReturn(account("BLOCKED", 1_000_000L, "1234"));

        gateway.lockAccount(ACCOUNT_ID);

        verify(mockBankMapper).getAccountForUpdate(ACCOUNT_ID);
    }

    @Test
    @DisplayName("존재하지 않는 계좌 잠금은 구분 없는 승인 오류로 거부한다")
    void lockAccountRejectsMissingAccount() {
        when(mockBankMapper.getAccountForUpdate(ACCOUNT_ID)).thenReturn(null);

        assertThrows(
                BankAccountForbiddenException.class,
                () -> gateway.lockAccount(ACCOUNT_ID)
        );
    }

    @Test
    @DisplayName("게이트웨이의 모든 작업은 기존 자금 트랜잭션을 필수로 요구한다")
    void gatewayRequiresExistingTransaction() {
        Transactional annotation =
                MockBankTransferGateway.class.getAnnotation(Transactional.class);

        assertEquals(Propagation.MANDATORY, annotation.propagation());
    }

    @Test
    @DisplayName("bankCode+accountNo로 존재하는 비귀속 계좌의 내부 ID를 식별한다")
    void resolveAccountIdReturnsMatchingAccount() {
        when(mockBankMapper.findAccountIdByBankCodeAndAccountNo(BANK_CODE, ACCOUNT_NO))
                .thenReturn(ACCOUNT_ID);

        assertEquals(ACCOUNT_ID, gateway.resolveAccountId(BANK_CODE, ACCOUNT_NO));
    }

    @Test
    @DisplayName("bankCode+accountNo에 일치하는 계좌가 없으면 승인 오류로 거부한다")
    void resolveAccountIdRejectsUnknownAccount() {
        when(mockBankMapper.findAccountIdByBankCodeAndAccountNo(BANK_CODE, ACCOUNT_NO))
                .thenReturn(null);

        assertThrows(
                BankAccountForbiddenException.class,
                () -> gateway.resolveAccountId(BANK_CODE, ACCOUNT_NO)
        );
    }

    @Test
    @DisplayName("충전 사전 검증은 PIN 불일치를 계좌 미존재·비활성과 같은 오류로 거부한다")
    void preflightRejectsPinMismatch() {
        MockBankAccount account = account("ACTIVE", 1_000_000L, PIN);
        when(mockBankMapper.getAccountById(ACCOUNT_ID)).thenReturn(account);

        assertThrows(
                BankAccountForbiddenException.class,
                () -> gateway.preflight(preflight("9999"))
        );
    }

    @Test
    @DisplayName("출금 방향 사전 검증은 PIN 없이 상태만 확인한다")
    void preflightSkipsPinCheckWhenPinAbsent() {
        MockBankAccount account = account("ACTIVE", 1_000_000L, PIN);
        when(mockBankMapper.getAccountById(ACCOUNT_ID)).thenReturn(account);

        gateway.preflight(preflight(null));
    }

    @Test
    @DisplayName("출금은 lockAccount()가 잡은 X-lock을 재사용해 비잠금 조회로 원장 결과를 반환한다")
    void withdrawReusesLockAndReturnsVerifiedResult() {
        when(mockBankMapper.getAccountById(ACCOUNT_ID))
                .thenReturn(account("ACTIVE", 1_000_000L, PIN));
        when(mockBankMapper.withdrawFromAccount(ACCOUNT_ID, AMOUNT)).thenReturn(1);
        when(mockBankMapper.insertBankTransaction(any())).thenAnswer(invocation -> {
            BankTransactionParam param = invocation.getArgument(0);
            param.setId(31L);
            return 1;
        });

        BankTransferResult result = gateway.withdraw(transferCommand(PIN));

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
    @DisplayName("잠금 후 PIN이 일치하지 않으면 계좌와 원장을 변경하지 않는다")
    void withdrawRejectsPinMismatchAfterLock() {
        when(mockBankMapper.getAccountById(ACCOUNT_ID))
                .thenReturn(account("ACTIVE", 1_000_000L, PIN));

        assertThrows(
                BankAccountForbiddenException.class,
                () -> gateway.withdraw(transferCommand("9999"))
        );

        verify(mockBankMapper, never()).withdrawFromAccount(ACCOUNT_ID, AMOUNT);
        verify(mockBankMapper, never()).insertBankTransaction(any());
    }

    @Test
    @DisplayName("잠금 후 계좌 잔액이 부족하면 계좌와 원장을 변경하지 않는다")
    void withdrawRejectsInsufficientBalance() {
        when(mockBankMapper.getAccountById(ACCOUNT_ID))
                .thenReturn(account("ACTIVE", AMOUNT - 1, PIN));

        assertThrows(
                InsufficientBankBalanceException.class,
                () -> gateway.withdraw(transferCommand(PIN))
        );

        verify(mockBankMapper, never()).withdrawFromAccount(ACCOUNT_ID, AMOUNT);
        verify(mockBankMapper, never()).insertBankTransaction(any());
    }

    @Test
    @DisplayName("은행 원장 INSERT 결과나 생성 ID가 없으면 성공 결과를 반환하지 않는다")
    void withdrawRejectsMissingLedgerIdentity() {
        when(mockBankMapper.getAccountById(ACCOUNT_ID))
                .thenReturn(account("ACTIVE", 1_000_000L, PIN));
        when(mockBankMapper.withdrawFromAccount(ACCOUNT_ID, AMOUNT)).thenReturn(1);
        when(mockBankMapper.insertBankTransaction(any())).thenReturn(1);

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.withdraw(transferCommand(PIN))
        );
    }

    @Test
    @DisplayName("0 이하 이체 금액은 계좌 잠금 전에 게이트웨이 계약 오류로 거부한다")
    void withdrawRejectsInvalidCommandBeforeLock() {
        BankTransferCommand command = BankTransferCommand.builder()
                .accountId(ACCOUNT_ID)
                .pin(PIN)
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
        MockBankAccount account = account(null, "ACTIVE", 1_000_000L, PIN);
        when(mockBankMapper.getAccountById(ACCOUNT_ID)).thenReturn(account);

        assertThrows(
                BankAccountForbiddenException.class,
                () -> gateway.preflight(preflight(PIN))
        );
    }

    @Test
    @DisplayName("잠금 계좌 UPDATE가 0건이면 잔액 부족이 아닌 서버 무결성 오류다")
    void withdrawRejectsUnexpectedUpdateCount() {
        when(mockBankMapper.getAccountById(ACCOUNT_ID))
                .thenReturn(account("ACTIVE", 1_000_000L, PIN));
        when(mockBankMapper.withdrawFromAccount(ACCOUNT_ID, AMOUNT)).thenReturn(0);

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.withdraw(transferCommand(PIN))
        );

        verify(mockBankMapper, never()).insertBankTransaction(any());
    }

    @Test
    @DisplayName("스키마 제약과 맞지 않는 계좌 잔액은 서버 무결성 오류다")
    void withdrawRejectsCorruptedAccountBalance() {
        MockBankAccount account = MockBankAccount.builder()
                .id(ACCOUNT_ID)
                .bankCode(BANK_CODE)
                .mockAccountNumber(ACCOUNT_NO)
                .pin(PIN)
                .balance(1_000_000L)
                .availableAmount(1_000_001L)
                .status("ACTIVE")
                .build();
        when(mockBankMapper.getAccountById(ACCOUNT_ID)).thenReturn(account);

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.withdraw(transferCommand(PIN))
        );

        verify(mockBankMapper, never()).withdrawFromAccount(ACCOUNT_ID, AMOUNT);
    }

    @Test
    @DisplayName("입금 후 잔액이 long 범위를 넘으면 계좌를 변경하지 않는다")
    void depositRejectsBalanceOverflow() {
        when(mockBankMapper.getAccountById(ACCOUNT_ID))
                .thenReturn(account("ACTIVE", Long.MAX_VALUE, PIN));

        assertThrows(
                BankTransferIntegrityException.class,
                () -> gateway.deposit(transferCommand(null))
        );

        verify(mockBankMapper, never()).depositToAccount(ACCOUNT_ID, AMOUNT);
        verify(mockBankMapper, never()).insertBankTransaction(any());
    }

    private BankAccountPreflightCommand preflight(String pin) {
        return BankAccountPreflightCommand.builder()
                .accountId(ACCOUNT_ID)
                .pin(pin)
                .build();
    }

    private BankTransferCommand transferCommand(String pin) {
        return BankTransferCommand.builder()
                .accountId(ACCOUNT_ID)
                .pin(pin)
                .amount(AMOUNT)
                .referenceType("FUNDING_ORDER")
                .referenceId(21L)
                .build();
    }

    private MockBankAccount account(String status, Long amount, String pin) {
        return account(ACCOUNT_ID, status, amount, pin);
    }

    private MockBankAccount account(Long id, String status, Long amount, String pin) {
        return MockBankAccount.builder()
                .id(id)
                .bankCode(BANK_CODE)
                .mockAccountNumber(ACCOUNT_NO)
                .pin(pin)
                .balance(amount)
                .availableAmount(amount)
                .status(status)
                .build();
    }
}
