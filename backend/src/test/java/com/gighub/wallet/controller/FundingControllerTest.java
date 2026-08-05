package com.gighub.wallet.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.bank.exception.BankAccountForbiddenException;
import com.gighub.bank.exception.InsufficientBankBalanceException;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.member.domain.UserRole;
import com.gighub.wallet.exception.IdempotencyKeyReusedException;
import com.gighub.wallet.service.FundingService;
import com.gighub.wallet.service.command.FundingCommand;
import com.gighub.wallet.service.result.FundingResult;
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
 * {@link FundingController}의 인증, 요청 검증, 응답 Envelope와 오류 매핑을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class FundingControllerTest {

    private static final Long EMPLOYER_ID = 3L;
    private static final String BANK_CODE = "004";
    private static final String ACCOUNT_NO = "1234567890";
    private static final String PIN = "0000";
    private static final Long AMOUNT = 100_000L;
    private static final String IDEMPOTENCY_KEY = "FUND-TEST-001";

    @Mock
    private FundingService fundingService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FundingController(fundingService))
                .setControllerAdvice(new CommonExceptionHandler())
                .build();

        AuthPrincipal principal = new AuthPrincipal(EMPLOYER_ID, UserRole.OWNER, "김사장");
        authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void newFundingReturnsCreatedWithoutBalanceOrPin() throws Exception {
        when(fundingService.fund(any())).thenReturn(FundingResult.builder()
                .fundingOrderId(21L)
                .status("COMPLETED")
                .bankTransactionId(31L)
                .availableBalance(1_000_000L)
                .lockedBalance(0L)
                .replayed(false)
                .build());

        mockMvc.perform(post("/api/wallet/funding-orders")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isCreated())
                .andExpect(header().doesNotExist("Idempotency-Replayed"))
                .andExpect(jsonPath("$.data.fundingOrderId").value(21))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.bankTransactionId").value(31))
                // 응답에 계좌 ID·PIN·잔액이 섞이면 안 된다(DEC-BANK-INPUT, DEC-FUNDING-PIN).
                .andExpect(jsonPath("$.data.pin").doesNotExist())
                .andExpect(jsonPath("$.data.bankAccountId").doesNotExist())
                .andExpect(jsonPath("$.data.availableBalance").doesNotExist())
                .andExpect(jsonPath("$.data.lockedBalance").doesNotExist());

        ArgumentCaptor<FundingCommand> captor = ArgumentCaptor.forClass(FundingCommand.class);
        verify(fundingService).fund(captor.capture());
        FundingCommand command = captor.getValue();
        assertEquals(EMPLOYER_ID, command.getEmployerId());
        assertEquals(BANK_CODE, command.getBankCode());
        assertEquals(ACCOUNT_NO, command.getAccountNo());
        assertEquals(PIN, command.getPin());
        assertEquals(AMOUNT, command.getAmount());
        assertEquals(IDEMPOTENCY_KEY, command.getIdempotencyKey());
    }

    @Test
    void replayedFundingReturnsOkWithReplayHeader() throws Exception {
        when(fundingService.fund(any())).thenReturn(FundingResult.builder()
                .fundingOrderId(21L)
                .status("COMPLETED")
                .bankTransactionId(31L)
                .availableBalance(1_000_000L)
                .lockedBalance(0L)
                .replayed(true)
                .build());

        mockMvc.perform(post("/api/wallet/funding-orders")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotency-Replayed", "true"))
                .andExpect(jsonPath("$.data.fundingOrderId").value(21));
    }

    @Test
    void rejectsRequestWithoutAuthentication() throws Exception {
        mockMvc.perform(post("/api/wallet/funding-orders")
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void rejectsRequestWithoutIdempotencyKeyHeader() throws Exception {
        mockMvc.perform(post("/api/wallet/funding-orders")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void rejectsUnapprovedBankCodeBeforeServiceCall() throws Exception {
        mockMvc.perform(post("/api/wallet/funding-orders")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bankCode": "999",
                                  "accountNo": "1234567890",
                                  "pin": "0000",
                                  "amount": 100000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("bankCode"));
    }

    @Test
    void translatesAccountForbiddenToApprovedEnvelope() throws Exception {
        when(fundingService.fund(any()))
                .thenThrow(new BankAccountForbiddenException("계좌를 사용할 수 없습니다."));

        mockMvc.perform(post("/api/wallet/funding-orders")
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
    void translatesInsufficientBankBalanceToConflict() throws Exception {
        when(fundingService.fund(any()))
                .thenThrow(new InsufficientBankBalanceException("연결 계좌의 가용 잔액이 부족합니다."));

        mockMvc.perform(post("/api/wallet/funding-orders")
                        .principal(authentication)
                        .header("Idempotency-Key", IDEMPOTENCY_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void translatesIdempotencyKeyReuseToConflict() throws Exception {
        when(fundingService.fund(any()))
                .thenThrow(new IdempotencyKeyReusedException("같은 멱등 키로 다른 충전 요청이 접수되었습니다."));

        mockMvc.perform(post("/api/wallet/funding-orders")
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
                  "pin": "%s",
                  "amount": %d
                }
                """.formatted(BANK_CODE, ACCOUNT_NO, PIN, AMOUNT);
    }
}
