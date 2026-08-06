package com.gighub.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.api.PageResponse;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.ValidationException;
import com.gighub.member.domain.UserRole;
import com.gighub.wallet.dto.WalletBalanceResponse;
import com.gighub.wallet.dto.WalletTransactionItem;
import com.gighub.wallet.service.WalletQueryService;
import com.gighub.wallet.service.command.WalletTransactionCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** {@link WalletController}의 인증, Query 검증과 외부 JSON 계약을 검증합니다. */
@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private static final Long USER_ID = 3L;

    @Mock
    private WalletQueryService walletQueryService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalletController(walletQueryService))
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(converter)
                .setValidator(validator)
                .build();

        AuthPrincipal principal = new AuthPrincipal(USER_ID, UserRole.OWNER, "김사장");
        authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void returnsWalletSummary() throws Exception {
        when(walletQueryService.getWallet(USER_ID)).thenReturn(
                WalletBalanceResponse.builder()
                        .currency("KRW")
                        .availableBalance(400_000L)
                        .lockedBalance(300_000L)
                        .build()
        );

        mockMvc.perform(get("/api/wallet").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data.currency").value("KRW"))
                .andExpect(jsonPath("$.data.availableBalance").value(400_000L))
                .andExpect(jsonPath("$.data.lockedBalance").value(300_000L));
    }

    @Test
    void rejectsWalletSummaryWithoutSession() throws Exception {
        mockMvc.perform(get("/api/wallet"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(walletQueryService, never()).getWallet(any());
    }

    @Test
    void returnsCommonNotFoundWhenWalletMissing() throws Exception {
        when(walletQueryService.getWallet(USER_ID))
                .thenThrow(new ResourceNotFoundException("지갑을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/wallet").principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    @Test
    void returnsApprovedTransactionItemAndPage() throws Exception {
        WalletTransactionItem item = WalletTransactionItem.builder()
                .transactionId(10L)
                .type("ESCROW_HOLD")
                .amount(300_000L)
                .direction("DEBIT")
                .availableAfter(400_000L)
                .lockedAfter(300_000L)
                .workCaseId(20L)
                .workTitle("주말 홀 서빙")
                .workplaceName("기가 허브")
                .displayStatus("COMPLETED")
                .createdAt(Instant.parse("2026-07-22T04:00:00Z"))
                .build();
        when(walletQueryService.getTransactions(eq(USER_ID), any()))
                .thenReturn(PageResponse.of(List.of(item), 0, 20, 1));

        mockMvc.perform(get("/api/wallet/transactions").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].length()").value(11))
                .andExpect(jsonPath("$.data.content[0].type").value("ESCROW_HOLD"))
                .andExpect(jsonPath("$.data.content[0].direction").value("DEBIT"))
                .andExpect(jsonPath("$.data.content[0].displayStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.content[0].createdAt")
                        .value("2026-07-22T04:00:00Z"))
                .andExpect(jsonPath("$.data.page.number").value(0))
                .andExpect(jsonPath("$.data.page.size").value(20))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }

    @Test
    void bindsApprovedQueryNamesAndDefaults() throws Exception {
        when(walletQueryService.getTransactions(eq(USER_ID), any()))
                .thenReturn(PageResponse.of(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/wallet/transactions")
                        .param("workplaceId", "7")
                        .param("from", "2026-07-01")
                        .param("to", "2026-07-31")
                        .param("type", "FUNDING")
                        .param("minAmount", "1000")
                        .param("maxAmount", "5000")
                        .param("keyword", " 허브 ")
                        .principal(authentication))
                .andExpect(status().isOk());

        ArgumentCaptor<WalletTransactionCriteria> captor =
                ArgumentCaptor.forClass(WalletTransactionCriteria.class);
        verify(walletQueryService).getTransactions(eq(USER_ID), captor.capture());
        WalletTransactionCriteria criteria = captor.getValue();
        assertEquals(7L, criteria.getWorkplaceId());
        assertEquals(LocalDate.of(2026, 7, 1), criteria.getFrom());
        assertEquals(LocalDate.of(2026, 7, 31), criteria.getTo());
        assertEquals("FUNDING", criteria.getType());
        assertEquals(1_000L, criteria.getMinAmount());
        assertEquals(5_000L, criteria.getMaxAmount());
        assertEquals(" 허브 ", criteria.getKeyword());
        assertEquals("LATEST", criteria.getSort());
        assertEquals(0, criteria.getPage());
        assertEquals(20, criteria.getSize());
    }

    @Test
    void rejectsUnknownSortWithFieldError() throws Exception {
        assertFieldValidation("sort", "id;DROP", "sort");
    }

    @Test
    void rejectsUnknownTypeWithFieldError() throws Exception {
        assertFieldValidation("type", "UNKNOWN_TYPE", "type");
    }

    @Test
    void rejectsNegativeMinAmountWithFieldError() throws Exception {
        assertFieldValidation("minAmount", "-1", "minAmount");
    }

    @Test
    void rejectsNegativePageWithFieldError() throws Exception {
        assertFieldValidation("page", "-1", "page");
    }

    @Test
    void rejectsPageSizeAboveLimitWithFieldError() throws Exception {
        assertFieldValidation("size", "101", "size");
    }

    @Test
    void rejectsMalformedDateWithFieldError() throws Exception {
        assertFieldValidation("from", "2026-99-99", "from");
    }

    @Test
    void exposesServiceCrossFieldValidationAsFieldError() throws Exception {
        when(walletQueryService.getTransactions(eq(USER_ID), any()))
                .thenThrow(new ValidationException(
                        "입력값을 확인해 주세요.",
                        "to",
                        "to는 from과 같거나 이후여야 합니다."
                ));

        mockMvc.perform(get("/api/wallet/transactions")
                        .param("from", "2026-07-24")
                        .param("to", "2026-07-22")
                        .principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("to"));
    }

    @Test
    void rejectsTransactionsWithoutSession() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(walletQueryService, never()).getTransactions(any(), any());
    }

    private void assertFieldValidation(
            String parameter,
            String value,
            String expectedField) throws Exception {
        mockMvc.perform(get("/api/wallet/transactions")
                        .param(parameter, value)
                        .principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.fieldErrors[0].field").value(expectedField));

        verify(walletQueryService, never()).getTransactions(any(), any());
    }
}
