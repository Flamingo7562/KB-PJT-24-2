package com.gighub.invitation.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.common.trace.TraceIdFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 초대 상태 오류가 명세의 상태·코드·메시지 그대로 나가는지 검증합니다.
 */
class InvitationErrorContractTest {

    /** 오류 응답에 섞이면 안 되는 Token 원문을 흉내 낸 값입니다. */
    private static final String RAW_TOKEN = "cnJhd0ludml0YXRpb25Ub2tlblNhbXBsZVZhbHVl";

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ThrowingController())
                .setControllerAdvice(new CommonExceptionHandler())
                .addFilters(new TraceIdFilter())
                .build();
    }

    @Test
    void expiredInvitationUsesGoneWithApprovedMessage() throws Exception {
        mockMvc.perform(get("/api/invitations/{token}/expired", RAW_TOKEN))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("INVITATION_EXPIRED"))
                .andExpect(jsonPath("$.message").value("초대 링크가 만료되었습니다."))
                .andExpect(jsonPath("$.traceId").isString())
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void revokedAndAcceptedAndChangedTermsUseDistinctConflictCodes() throws Exception {
        mockMvc.perform(get("/api/invitations/{token}/revoked", RAW_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_REVOKED"))
                .andExpect(jsonPath("$.message").value("철회된 초대 링크입니다."));

        mockMvc.perform(get("/api/invitations/{token}/accepted", RAW_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_ALREADY_ACCEPTED"))
                .andExpect(jsonPath("$.message").value("이미 수락된 초대 링크입니다."));

        mockMvc.perform(get("/api/invitations/{token}/terms-changed", RAW_TOKEN))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_TERMS_CHANGED"))
                .andExpect(jsonPath("$.message").value("근무 조건이 변경되어 초대를 사용할 수 없습니다."));
    }

    @Test
    void missingInvitationUsesNotFoundWithoutRevealingExistence() throws Exception {
        mockMvc.perform(get("/api/invitations/{token}/not-found", RAW_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("초대 링크를 찾을 수 없습니다."));
    }

    @Test
    void invitationErrorMessagesAreFixedAndNeverEchoTheToken() {
        List<ApiException> exceptions = List.of(
                new InvitationNotFoundException(),
                new InvitationExpiredException(),
                new InvitationRevokedException(),
                new InvitationAlreadyAcceptedException(),
                new InvitationTermsChangedException()
        );

        for (ApiException exception : exceptions) {
            // 메시지를 호출부에서 받지 않으므로 Token이나 내부 식별자가 섞일 경로 자체가 없습니다.
            assertFalse(exception.getMessage().contains(RAW_TOKEN));

            // 생성자가 메시지를 받지 않아야 호출부가 Token을 덧붙일 방법이 없습니다.
            Constructor<?>[] constructors = exception.getClass().getConstructors();
            assertEquals(1, constructors.length);
            assertEquals(0, constructors[0].getParameterCount());
        }
    }

    @Test
    void invitationCodesKeepTheirApprovedHttpStatus() {
        assertEquals(HttpStatus.NOT_FOUND, new InvitationNotFoundException().getStatus());
        assertEquals(ApiErrorCode.RESOURCE_NOT_FOUND, new InvitationNotFoundException().getCode());

        assertEquals(HttpStatus.GONE, new InvitationExpiredException().getStatus());
        assertEquals(ApiErrorCode.INVITATION_EXPIRED, new InvitationExpiredException().getCode());

        assertEquals(HttpStatus.CONFLICT, new InvitationRevokedException().getStatus());
        assertEquals(ApiErrorCode.INVITATION_REVOKED, new InvitationRevokedException().getCode());

        assertEquals(HttpStatus.CONFLICT, new InvitationAlreadyAcceptedException().getStatus());
        assertEquals(
                ApiErrorCode.INVITATION_ALREADY_ACCEPTED,
                new InvitationAlreadyAcceptedException().getCode()
        );

        assertEquals(HttpStatus.CONFLICT, new InvitationTermsChangedException().getStatus());
        assertEquals(
                ApiErrorCode.INVITATION_TERMS_CHANGED,
                new InvitationTermsChangedException().getCode()
        );
    }

    @RestController
    private static class ThrowingController {

        @GetMapping("/api/invitations/{token}/not-found")
        public void notFound(@PathVariable String token) {
            throw new InvitationNotFoundException();
        }

        @GetMapping("/api/invitations/{token}/expired")
        public void expired(@PathVariable String token) {
            throw new InvitationExpiredException();
        }

        @GetMapping("/api/invitations/{token}/revoked")
        public void revoked(@PathVariable String token) {
            throw new InvitationRevokedException();
        }

        @GetMapping("/api/invitations/{token}/accepted")
        public void accepted(@PathVariable String token) {
            throw new InvitationAlreadyAcceptedException();
        }

        @GetMapping("/api/invitations/{token}/terms-changed")
        public void termsChanged(@PathVariable String token) {
            throw new InvitationTermsChangedException();
        }
    }
}
