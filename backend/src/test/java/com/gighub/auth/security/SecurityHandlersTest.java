package com.gighub.auth.security;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SecurityHandlersTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticationEntryPointWritesApprovedUnauthorizedEnvelope() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JsonAuthenticationEntryPoint().commence(
                request,
                response,
                new InsufficientAuthenticationException("internal detail")
        );

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEnvelope(response, "AUTH_REQUIRED", "인증이 필요합니다.");
    }

    @Test
    void accessDeniedHandlerWritesApprovedForbiddenEnvelope() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        new JsonAccessDeniedHandler().handle(
                request,
                response,
                new AccessDeniedException("internal detail")
        );

        assertEquals(403, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        assertEnvelope(response, "FORBIDDEN", "요청을 수행할 권한이 없습니다.");
    }

    private void assertEnvelope(MockHttpServletResponse response, String code, String message) throws Exception {
        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertEquals(code, body.get("code").asText());
        assertEquals(message, body.get("message").asText());
        assertNotNull(UUID.fromString(body.get("traceId").asText()));
        assertFalse(body.has("fieldErrors"));
    }
}
