package com.gighub.invitation.controller;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.common.exception.ConflictException;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.common.exception.WorkCaseLockedException;
import com.gighub.common.trace.TraceIdFilter;
import com.gighub.config.ApiJsonMapper;
import com.gighub.invitation.dto.InvitationIssueResponse;
import com.gighub.invitation.service.InvitationIssueResult;
import com.gighub.invitation.service.InvitationIssueService;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 초대 발급 Endpoint의 HTTP 계약을 확인합니다.
 */
class WorkCaseInvitationControllerTest {

    private static final long WORK_CASE_ID = 7L;
    private static final String INVITE_URL =
            "https://app.example.com/invitations/3rXQ0Zk8m1UvJ2Nw6bTyaPcLdEfGhIjKlMnOpQrStUv";

    private InvitationIssueService invitationIssueService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        invitationIssueService = mock(InvitationIssueService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new WorkCaseInvitationController(invitationIssueService))
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(ApiJsonMapper.create()))
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void newInvitationIsCreatedWithTheApprovedEnvelope() throws Exception {
        when(invitationIssueService.issue(any(), eq(WORK_CASE_ID)))
                .thenReturn(InvitationIssueResult.created(response()));

        mockMvc.perform(post("/api/work-cases/{workCaseId}/invitations", WORK_CASE_ID)
                        .principal(ownerAuthentication()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inviteUrl").value(INVITE_URL))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-08-20T01:00:00Z"))
                .andExpect(jsonPath("$.data.token").doesNotExist());
    }

    @Test
    void existingActiveInvitationUsesOkWithTheSameBody() throws Exception {
        when(invitationIssueService.issue(any(), eq(WORK_CASE_ID)))
                .thenReturn(InvitationIssueResult.existing(response()));

        mockMvc.perform(post("/api/work-cases/{workCaseId}/invitations", WORK_CASE_ID)
                        .principal(ownerAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.inviteUrl").value(INVITE_URL))
                .andExpect(jsonPath("$.data.expiresAt").value("2026-08-20T01:00:00Z"));
    }

    @Test
    void requestBodyIsIgnoredSoClientsCannotChooseServerDecidedValues() throws Exception {
        when(invitationIssueService.issue(any(), eq(WORK_CASE_ID)))
                .thenReturn(InvitationIssueResult.created(response()));

        // Body를 실어 보내도 대상 사용자·Version·만료·Token은 서버 결정 값 그대로여야 합니다.
        mockMvc.perform(post("/api/work-cases/{workCaseId}/invitations", WORK_CASE_ID)
                        .principal(ownerAuthentication())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":9,\"expiresAt\":\"2099-01-01T00:00:00Z\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.expiresAt").value("2026-08-20T01:00:00Z"));

        verify(invitationIssueService).issue(any(AuthPrincipal.class), eq(WORK_CASE_ID));
    }

    @Test
    void unauthenticatedRequestUsesTheCommonAuthContract() throws Exception {
        mockMvc.perform(post("/api/work-cases/{workCaseId}/invitations", WORK_CASE_ID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void issueFailuresKeepTheirApprovedStatusAndCode() throws Exception {
        assertFailure(new RoleMismatchException("초대는 OWNER만 발급할 수 있습니다."),
                403, "ROLE_MISMATCH");
        assertFailure(new ResourceNotFoundException("근무 Case를 찾을 수 없습니다."),
                404, "RESOURCE_NOT_FOUND");
        assertFailure(new WorkCaseLockedException("초대를 발급할 수 없는 근무입니다."),
                409, "WORK_CASE_LOCKED");
        assertFailure(new ConflictException("초대 상태를 다시 확인해 주세요."),
                409, "CONFLICT");
    }

    private void assertFailure(RuntimeException failure, int status, String code) throws Exception {
        doThrow(failure).when(invitationIssueService).issue(any(), eq(WORK_CASE_ID));

        mockMvc.perform(post("/api/work-cases/{workCaseId}/invitations", WORK_CASE_ID)
                        .principal(ownerAuthentication()))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    private static InvitationIssueResponse response() {
        return InvitationIssueResponse.of(INVITE_URL, Instant.parse("2026-08-20T01:00:00Z"));
    }

    private static Authentication ownerAuthentication() {
        return new UsernamePasswordAuthenticationToken(
                new AuthPrincipal(3L, UserRole.OWNER, "김사장"), "N/A", List.of());
    }
}
