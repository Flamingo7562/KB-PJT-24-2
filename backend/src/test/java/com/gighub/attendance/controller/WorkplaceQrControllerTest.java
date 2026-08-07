package com.gighub.attendance.controller;

import java.time.Instant;
import java.util.List;

import com.gighub.attendance.dto.WorkplaceQrResponse;
import com.gighub.attendance.exception.WorkplaceQrIntegrityException;
import com.gighub.attendance.service.WorkplaceQrService;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.config.ApiJsonMapper;
import com.gighub.common.exception.ResourceNotFoundException;
import com.gighub.common.exception.RoleMismatchException;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 고정 QR Endpoint의 승인 응답과 오류 Code를 검증합니다. */
class WorkplaceQrControllerTest {

    private WorkplaceQrService workplaceQrService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        workplaceQrService = mock(WorkplaceQrService.class);
        // 운영 MessageConverter를 그대로 씁니다. 기본 ObjectMapper를 쓰면 Instant가 epoch
        // 숫자로 나가 시점 계약이 실제 응답과 달라집니다.
        mockMvc = MockMvcBuilders.standaloneSetup(new WorkplaceQrController(workplaceQrService))
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(ApiJsonMapper.create()))
                .build();
    }

    @Test
    void findReturnsApprovedQrEnvelope() throws Exception {
        when(workplaceQrService.findQr(any(), eq(1L))).thenReturn(
                new WorkplaceQrResponse(1L, "v1.k1.1.nonce.mac",
                        Instant.parse("2026-07-31T00:00:00Z")));

        mockMvc.perform(get("/api/workplaces/1/qr").principal(ownerAuthentication()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workplaceId").value(1))
                .andExpect(jsonPath("$.data.qrToken").value("v1.k1.1.nonce.mac"))
                .andExpect(jsonPath("$.data.createdAt").value("2026-07-31T00:00:00Z"));
    }

    @Test
    void findReportsRoleMismatchAsForbidden() throws Exception {
        when(workplaceQrService.findQr(any(), any()))
                .thenThrow(new RoleMismatchException("사업장 QR은 OWNER만 사용할 수 있습니다."));

        mockMvc.perform(get("/api/workplaces/1/qr").principal(ownerAuthentication()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_MISMATCH"));
    }

    @Test
    void findReportsForeignWorkplaceAsNotFound() throws Exception {
        when(workplaceQrService.findQr(any(), any()))
                .thenThrow(new ResourceNotFoundException("사업장을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/workplaces/1/qr").principal(ownerAuthentication()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void findReportsMissingActiveQrAsInternalErrorWithTraceId() throws Exception {
        when(workplaceQrService.findQr(any(), any()))
                .thenThrow(new WorkplaceQrIntegrityException("활성 고정 QR이 없습니다."));

        mockMvc.perform(get("/api/workplaces/1/qr").principal(ownerAuthentication()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    private static Authentication ownerAuthentication() {
        AuthPrincipal principal = new AuthPrincipal(7L, UserRole.OWNER, "김사장");
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
