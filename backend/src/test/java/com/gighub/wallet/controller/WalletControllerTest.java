package com.gighub.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.wallet.dto.WalletSummary;
import com.gighub.wallet.dto.WalletTransactionSearch;
import com.gighub.wallet.dto.WalletTransactionView;
import com.gighub.wallet.mapper.WalletQueryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link WalletController}의 인증, 필터 검증, 응답 변환을 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    private static final String LOGIN_USER = "LOGIN_USER";
    private static final Long USER_ID = 3L;

    @Mock
    private WalletQueryMapper walletQueryMapper;

    private MockMvc mockMvc;
    private MockHttpSession session;

    /**
     * Java Time 직렬화를 포함한 독립형 Spring MVC 테스트 환경을 준비합니다.
     */
    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WalletController(walletQueryMapper))
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(converter)
                .build();

        session = new MockHttpSession();
        session.setAttribute(LOGIN_USER, USER_ID);
    }

    private WalletSummary summary(long available, long locked) {
        WalletSummary walletSummary = new WalletSummary();
        walletSummary.setWalletId(30L);
        walletSummary.setAvailableBalance(available);
        walletSummary.setLockedBalance(locked);
        return walletSummary;
    }

    private WalletTransactionView view(String type, long amount) {
        WalletTransactionView row = new WalletTransactionView();
        row.setTransactionId(10L);
        row.setType(type);
        row.setAmount(amount);
        row.setAvailableAfter(400_000L);
        row.setLockedAfter(0L);
        row.setCreatedAt(LocalDateTime.of(2026, 7, 22, 13, 0));
        return row;
    }

    // ===================== GET /api/wallet =====================

    /**
     * 지갑 요약이 가용 잔액과 잠금 잔액을 분리해 반환하는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void returnsWalletSummary() throws Exception {
        when(walletQueryMapper.findWalletSummaryByUserId(USER_ID))
                .thenReturn(summary(400_000L, 300_000L));

        mockMvc.perform(get("/api/wallet").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currency").value("KRW"))
                .andExpect(jsonPath("$.data.availableBalance").value(400_000L))
                .andExpect(jsonPath("$.data.lockedBalance").value(300_000L));
    }

    /**
     * 비로그인 요청이 지갑을 조회하지 않고 401을 반환하는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsWalletSummaryWithoutSession() throws Exception {
        mockMvc.perform(get("/api/wallet"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(walletQueryMapper, never()).findWalletSummaryByUserId(any());
    }

    /**
     * 지갑이 없는 사용자에게 404를 반환하는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void returnsNotFoundWhenWalletMissing() throws Exception {
        when(walletQueryMapper.findWalletSummaryByUserId(USER_ID)).thenReturn(null);

        mockMvc.perform(get("/api/wallet").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.traceId").isString());
    }

    // ===================== GET /api/wallet/transactions =====================

    /**
     * 거래 내역이 파생 상태와 페이지 정보를 포함해 반환되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void returnsTransactionPage() throws Exception {
        when(walletQueryMapper.countTransactions(any())).thenReturn(1L);
        when(walletQueryMapper.findTransactions(any()))
                .thenReturn(List.of(view("ESCROW_HOLD", 300_000L)));

        mockMvc.perform(get("/api/wallet/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("ESCROW_HOLD"))
                .andExpect(jsonPath("$.data.content[0].displayStatus").value("예치중"))
                .andExpect(jsonPath("$.data.content[0].createdAt").value("2026-07-22T04:00:00Z"))
                .andExpect(jsonPath("$.data.page.number").value(0))
                .andExpect(jsonPath("$.data.page.size").value(20))
                .andExpect(jsonPath("$.data.page.totalElements").value(1))
                .andExpect(jsonPath("$.data.page.totalPages").value(1));
    }

    /**
     * 조회 결과가 없으면 목록 쿼리를 호출하지 않는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void skipsListQueryWhenCountIsZero() throws Exception {
        when(walletQueryMapper.countTransactions(any())).thenReturn(0L);

        mockMvc.perform(get("/api/wallet/transactions").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.page.totalPages").value(0));

        verify(walletQueryMapper, never()).findTransactions(any());
    }

    /**
     * to 조건이 해당 일자를 포함하도록 다음 날 자정 미만으로 변환되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void convertsToDateAsExclusiveUpperBound() throws Exception {
        when(walletQueryMapper.countTransactions(any())).thenReturn(0L);

        mockMvc.perform(get("/api/wallet/transactions")
                        .param("from", "2026-07-22")
                        .param("to", "2026-07-22")
                        .session(session))
                .andExpect(status().isOk());

        ArgumentCaptor<WalletTransactionSearch> captor =
                ArgumentCaptor.forClass(WalletTransactionSearch.class);
        verify(walletQueryMapper).countTransactions(captor.capture());

        WalletTransactionSearch search = captor.getValue();
        assertEquals(LocalDateTime.of(2026, 7, 22, 0, 0), search.getFrom());
        assertEquals(LocalDateTime.of(2026, 7, 23, 0, 0), search.getToExclusive());
    }

    /**
     * page와 size로 계산한 offset이 조회 조건에 반영되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void appliesOffsetFromPageAndSize() throws Exception {
        when(walletQueryMapper.countTransactions(any())).thenReturn(0L);

        mockMvc.perform(get("/api/wallet/transactions")
                        .param("page", "2")
                        .param("size", "10")
                        .session(session))
                .andExpect(status().isOk());

        ArgumentCaptor<WalletTransactionSearch> captor =
                ArgumentCaptor.forClass(WalletTransactionSearch.class);
        verify(walletQueryMapper).countTransactions(captor.capture());

        assertEquals(20, captor.getValue().getOffset());
        assertEquals(10, captor.getValue().getSize());
    }

    /**
     * 공백만 있는 keyword가 조건에서 제외되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void treatsBlankKeywordAsAbsent() throws Exception {
        when(walletQueryMapper.countTransactions(any())).thenReturn(0L);

        mockMvc.perform(get("/api/wallet/transactions")
                        .param("keyword", "   ")
                        .session(session))
                .andExpect(status().isOk());

        ArgumentCaptor<WalletTransactionSearch> captor =
                ArgumentCaptor.forClass(WalletTransactionSearch.class);
        verify(walletQueryMapper).countTransactions(captor.capture());

        assertEquals(null, captor.getValue().getKeyword());
    }

    // ===================== 필터 검증 =====================

    /**
     * whitelist에 없는 정렬 키가 쿼리 실행 전에 차단되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsUnknownSortKey() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions")
                        .param("sort", "id;DROP")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").isString());

        verify(walletQueryMapper, never()).countTransactions(any());
    }

    /**
     * 허용되지 않은 거래 유형이 차단되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsUnknownTransactionType() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions")
                        .param("type", "UNKNOWN_TYPE")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(walletQueryMapper, never()).countTransactions(any());
    }

    /**
     * 최대 페이지 크기를 초과한 요청이 차단되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsPageSizeAboveLimit() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions")
                        .param("size", "1000")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(walletQueryMapper, never()).countTransactions(any());
    }

    /**
     * 음수 페이지 번호가 차단되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions")
                        .param("page", "-1")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    /**
     * from이 to보다 뒤인 기간 조건이 차단되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsReversedDateRange() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions")
                        .param("from", "2026-07-24")
                        .param("to", "2026-07-22")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    /**
     * minAmount가 maxAmount보다 큰 금액 조건이 차단되는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsReversedAmountRange() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions")
                        .param("minAmount", "500000")
                        .param("maxAmount", "100000")
                        .session(session))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    /**
     * 비로그인 요청이 거래 내역을 조회하지 않고 401을 반환하는지 검증합니다.
     *
     * @throws Exception MockMvc 요청 실행에 실패한 경우
     */
    @Test
    void rejectsTransactionsWithoutSession() throws Exception {
        mockMvc.perform(get("/api/wallet/transactions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(walletQueryMapper, never()).countTransactions(any());
    }
}
