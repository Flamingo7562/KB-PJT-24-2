package com.gighub.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.member.domain.UserRole;
import com.gighub.settlement.service.SettlementService;
import com.gighub.settlement.service.command.SettlementApproveCommand;
import com.gighub.settlement.service.result.SettlementResult;
import com.gighub.wallet.service.EscrowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
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

class EscrowControllerTest {

    private static final Long EMPLOYER_ID = 3L;
    private static final Long WORK_CASE_ID = 1L;
    private static final LocalDateTime COMPLETED_AT =
            LocalDateTime.of(2026, 7, 24, 17, 12, 34, 123_456_000);

    private EscrowService escrowService;
    private SettlementService settlementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        escrowService = mock(EscrowService.class);
        settlementService = mock(SettlementService.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new EscrowController(
                        escrowService,
                        settlementService
                ))
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(objectMapper)
                )
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
        Authentication authentication = employerAuthentication();

        mockMvc.perform(post(
                        "/api/work-cases/{workCaseId}/settlement/approve",
                        WORK_CASE_ID
                )
                        .principal(authentication)
                        .header("Idempotency-Key", "SETTLEMENT-KEY-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementId").value(12))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.completedAt")
                        .value("2026-07-24T08:12:34.123456Z"))
                .andExpect(jsonPath("$.data.replayed").doesNotExist());

        ArgumentCaptor<SettlementApproveCommand> captor =
                ArgumentCaptor.forClass(SettlementApproveCommand.class);
        verify(settlementService).approve(captor.capture());
        assertEquals(WORK_CASE_ID, captor.getValue().getWorkCaseId());
        assertEquals(EMPLOYER_ID, captor.getValue().getApproverUserId());
        assertEquals(
                "SETTLEMENT-KEY-001",
                captor.getValue().getIdempotencyKey()
        );
    }

    @Test
    void approveSettlementRequiresLoginBeforeServiceCall() throws Exception {
        mockMvc.perform(post(
                        "/api/work-cases/{workCaseId}/settlement/approve",
                        WORK_CASE_ID
                )
                        .header("Idempotency-Key", "SETTLEMENT-KEY-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        verify(settlementService, never()).approve(any());
    }

    @Test
    void holdRejectsDifferentWorkerThroughCommonErrorFlow() throws Exception {
        Authentication authentication = employerAuthentication();

        mockMvc.perform(post("/api/invites/{token}/accept", "test-token")
                        .principal(authentication)
                        .header("Idempotency-Key", "ESCROW-HOLD-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "employerId": 3,
                                  "workerId": 9,
                                  "workCaseId": 1,
                                  "amount": 100000
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").isString());

        verify(escrowService, never()).hold(any());
    }

    private static Authentication employerAuthentication() {
        AuthPrincipal principal = new AuthPrincipal(EMPLOYER_ID, UserRole.OWNER, "김사장");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
