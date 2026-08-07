package com.gighub.settlement.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.config.ApiJsonMapper;
import com.gighub.member.domain.UserRole;
import com.gighub.settlement.service.SettlementService;
import com.gighub.settlement.service.command.SettlementApproveCommand;
import com.gighub.settlement.service.result.SettlementResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 정산 승인 Endpoint의 HTTP 계약을 확인합니다.
 *
 * <p>구형 예치 Endpoint가 사라지면서 같은 Controller에 있던 정산 검증을 이 파일로
 * 옮겼습니다. 경로와 응답 계약은 그대로입니다.</p>
 */
class SettlementControllerTest {

    private static final Long EMPLOYER_ID = 3L;
    private static final Long WORK_CASE_ID = 1L;
    private static final String PATH = "/api/work-cases/{workCaseId}/settlement/approve";
    private static final String KEY = "SETTLEMENT-KEY-001";
    private static final LocalDateTime COMPLETED_AT =
            LocalDateTime.of(2026, 7, 24, 17, 12, 34, 123_456_000);

    private SettlementService settlementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        settlementService = mock(SettlementService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SettlementController(settlementService))
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(ApiJsonMapper.create()))
                .build();
    }

    @Test
    void approveSettlementReturnsStoredResultFields() throws Exception {
        when(settlementService.approve(any())).thenReturn(
                SettlementResult.builder()
                        .settlementId(12L)
                        .status("COMPLETED")
                        .completedAt(COMPLETED_AT)
                        .replayed(false)
                        .build()
        );

        mockMvc.perform(post(PATH, WORK_CASE_ID)
                        .principal(employerAuthentication())
                        .header("Idempotency-Key", KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementId").value(12))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt").value("2026-07-24T08:12:34.123456Z"))
                .andExpect(jsonPath("$.data.replayed").doesNotExist());

        ArgumentCaptor<SettlementApproveCommand> captor =
                ArgumentCaptor.forClass(SettlementApproveCommand.class);
        verify(settlementService).approve(captor.capture());
        assertEquals(WORK_CASE_ID, captor.getValue().getWorkCaseId());
        assertEquals(EMPLOYER_ID, captor.getValue().getApproverUserId());
        assertEquals(KEY, captor.getValue().getIdempotencyKey());
    }

    @Test
    void approveSettlementRequiresLoginBeforeServiceCall() throws Exception {
        mockMvc.perform(post(PATH, WORK_CASE_ID).header("Idempotency-Key", KEY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(settlementService, never()).approve(any());
    }

    private static Authentication employerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(EMPLOYER_ID, UserRole.OWNER, "김사장"), "N/A", List.of());
    }
}
