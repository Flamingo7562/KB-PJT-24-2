package com.gighub.bank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gighub.bank.dto.BankAccountSummary;
import com.gighub.bank.mapper.MockBankQueryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MockBankAccountController}의 인증, 소유자 한정 조회, status 필터를 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class MockBankAccountControllerTest {

    private static final String LOGIN_USER = "LOGIN_USER";
    private static final Long USER_ID = 3L;

    @Mock
    private MockBankQueryMapper mockBankQueryMapper;

    private MockMvc mockMvc;
    private MockHttpSession session;

    /**
     * Java Time 직렬화를 포함한 독립형 Spring MVC 테스트 환경을 준비합니다.
     */
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MockBankAccountController(mockBankQueryMapper))
                .setMessageConverters(converter)
                .build();

        session = new MockHttpSession();
        session.setAttribute(LOGIN_USER, USER_ID);
    }

    private BankAccountSummary account(long id, String maskedNumber, long balance) {
        BankAccountSummary summary = new BankAccountSummary();
        summary.setBankAccountId(id);
        summary.setBankCode("004");
        summary.setMaskedAccountNumber(maskedNumber);
        summary.setCurrency("KRW");
        summary.setBalance(balance);
        summary.setAvailableAmount(balance);
        summary.setStatus("ACTIVE");
        return summary;
    }

    /**
     * 계좌 목록이 마스킹된 번호와 잔액을 포함해 반환되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void returnsMaskedAccountList() throws Exception {
        when(mockBankQueryMapper.findAccountsByUserId(eq(USER_ID), isNull()))
                .thenReturn(List.of(account(1L, "********0001", 300_000L)));

        mockMvc.perform(get("/api/mock-bank-accounts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].bankAccountId").value(1))
                .andExpect(jsonPath("$.data.items[0].bankCode").value("004"))
                .andExpect(jsonPath("$.data.items[0].maskedAccountNumber").value("********0001"))
                .andExpect(jsonPath("$.data.items[0].currency").value("KRW"))
                .andExpect(jsonPath("$.data.items[0].balance").value(300_000L))
                .andExpect(jsonPath("$.data.items[0].availableAmount").value(300_000L))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));
    }

    /**
     * 응답에 원문 계좌번호 필드가 포함되지 않는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void doesNotExposeRawAccountNumber() throws Exception {
        when(mockBankQueryMapper.findAccountsByUserId(eq(USER_ID), isNull()))
                .thenReturn(List.of(account(1L, "********0001", 300_000L)));

        mockMvc.perform(get("/api/mock-bank-accounts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].mockAccountNumber").doesNotExist())
                .andExpect(jsonPath("$.data.items[0].accountNumber").doesNotExist());
    }

    /**
     * 조회 대상이 로그인 사용자로 한정되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void queriesOnlyLoginUserAccounts() throws Exception {
        when(mockBankQueryMapper.findAccountsByUserId(eq(USER_ID), isNull()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/mock-bank-accounts").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isEmpty());

        verify(mockBankQueryMapper).findAccountsByUserId(USER_ID, null);
    }

    /**
     * status=ACTIVE 필터가 조회 조건으로 전달되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void passesActiveStatusFilter() throws Exception {
        when(mockBankQueryMapper.findAccountsByUserId(eq(USER_ID), eq("ACTIVE")))
                .thenReturn(List.of(account(1L, "********0001", 300_000L)));

        mockMvc.perform(get("/api/mock-bank-accounts")
                        .param("status", "ACTIVE")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));

        verify(mockBankQueryMapper).findAccountsByUserId(USER_ID, "ACTIVE");
    }

    /**
     * 허용되지 않은 status 값이 쿼리 실행 전에 차단되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsUnsupportedStatusFilter() throws Exception {
        mockMvc.perform(get("/api/mock-bank-accounts")
                        .param("status", "CLOSED")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_FILTER"));

        verify(mockBankQueryMapper, never()).findAccountsByUserId(any(), any());
    }

    /**
     * 비로그인 요청이 계좌를 조회하지 않고 401을 반환하는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsRequestWithoutSession() throws Exception {
        mockMvc.perform(get("/api/mock-bank-accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(mockBankQueryMapper, never()).findAccountsByUserId(any(), any());
    }
}