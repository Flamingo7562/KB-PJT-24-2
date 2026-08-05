package com.gighub.wallet.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.member.domain.UserRole;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.exception.InsufficientAvailableBalanceException;
import com.gighub.wallet.service.WithdrawalService;
import com.gighub.wallet.service.command.WithdrawalCommand;
import com.gighub.wallet.service.result.WithdrawalResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link WithdrawalController}의 인증, 요청 검증, 응답 Envelope와 오류 매핑을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class WithdrawalControllerTest {

    private static final Long USER_ID = 4L;
    private static final String BANK_CODE = "004";
    private static final String ACCOUNT_NO = "1234567890";
    private static final Long AMOUNT = 100_000L;
    private static final String IDEMPOTENCY_KEY = "WD-TEST-001";

    @Mock
    private WithdrawalService withdrawalService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WithdrawalController(withdrawalService))
                .setControllerAdvice(new CommonExceptionHandler())
                .build();

        AuthPrincipal principal = new AuthPrincipal(USER_ID, UserRole.WORKER, "김근로");
        authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void newWithdrawalReturnsCreatedWithoutBalance() throws Exception {
        when(withdrawalService.withdraw(any())).thenReturn(WithdrawalResult.builder()
                .withdrawalRequestId(22L)
                .status("COMPLETED")
                .bankTransactionId(33L)
                .replayed(false)
                .build());

        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.data.withdrawalRequestId").value(22))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.bankTransactionId").value(33))
                // 잔액은 GET /api/wallet에서 다시 조회한다 - 응답에 섞이면 안 된다.
                .andExpect(jsonPath("$.data.availableBalance").doesNotExist())
                .andExpect(jsonPath("$.data.bankAccountId").doesNotExist());

        ArgumentCaptor<WithdrawalCommand> captor =
                ArgumentCaptor.forClass(WithdrawalCommand.class);
        verify(withdrawalService).withdraw(captor.capture());
        WithdrawalCommand command = captor.getValue();
        assertEquals(USER_ID, command.getUserId());
        assertEquals(BANK_CODE, command.getBankCode());
        assertEquals(ACCOUNT_NO, command.getAccountNo());
        assertEquals(AMOUNT, command.getAmount());
        assertEquals(IDEMPOTENCY_KEY, command.getIdempotencyKey());
    }

    @Test
    void replayedWithdrawalReturnsOkWithReplayHeader() throws Exception {
        when(withdrawalService.withdraw(any())).thenReturn(WithdrawalResult.builder()
                .withdrawalRequestId(22L)
                .status("COMPLETED")
                .bankTransactionId(33L)
                .replayed(true)
                .build());

        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.data.withdrawalRequestId").value(22));
    }

    @Test
    void rejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void rejectsRequestWithoutIdempotencyKeyHeader() throws Exception {
        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsUnapprovedBankCodeBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "999",
                                  "accountNo": "1234567890",
                                  "amount": 100000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("bankCode"));
    }

    @Test
    void rejectsRequestBodyContainingPin() throws Exception {
        // 출금 요청 DTO에는 pin 필드가 없다(DEC-WITHDRAWAL-DESTINATION) - Unknown 필드는
        // Jackson이 기본 무시하므로 여기서는 서비스가 pin 없이 호출되는지를 확인한다.
        when(withdrawalService.withdraw(any())).thenReturn(WithdrawalResult.builder()
                .withdrawalRequestId(22L)
                .status("COMPLETED")
                .bankTransactionId(33L)
                .replayed(false)
                .build());

        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "004",
                                  "accountNo": "1234567890",
                                  "pin": "0000",
                                  "amount": 100000
                                }
                                """))
                .andExpect(status().isCreated());

        ArgumentCaptor<WithdrawalCommand> captor =
                ArgumentCaptor.forClass(WithdrawalCommand.class);
        verify(withdrawalService).withdraw(captor.capture());
        assertEquals(USER_ID, captor.getValue().getUserId());
    }

    @Test
    void translatesAccountForbiddenToApprovedEnvelope() throws Exception {
        when(withdrawalService.withdraw(any()))
                .thenThrow(new BankAccountForbiddenException("계좌를 사용할 수 없습니다."));

        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("계좌를 사용할 수 없습니다."))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void translatesInsufficientAvailableBalanceToConflict() throws Exception {
        when(withdrawalService.withdraw(any()))
                .thenThrow(new InsufficientAvailableBalanceException("지갑의 가용 잔액이 부족합니다."));

        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void translatesIdempotencyKeyReuseToConflict() throws Exception {
        when(withdrawalService.withdraw(any()))
                .thenThrow(new IdempotencyKeyReusedException("같은 멱등 키로 다른 출금 요청이 접수되었습니다."));

        mockMvc.perform(post("/api/wallet/withdrawal-requests")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
    }

    private String requestBody() {
        return """
                {
                  "bankCode": "%s",
                  "accountNo": "%s",
                  "amount": %d
                }
                """.formatted(BANK_CODE, ACCOUNT_NO, AMOUNT);
    }
}
