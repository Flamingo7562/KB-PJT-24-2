package com.gighub.auth.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonAuthenticationEntryPointTest {

    private final JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint();

    @Test
    void returnsAuthRequiredWhenNoSessionWasAttempted() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("no auth"));

        assertEquals(401, response.getStatus());
        assertEquals("AUTH_REQUIRED", JsonPath.read(response.getContentAsString(), "$.code"));
    }

    @Test
    void returnsSessionExpiredWhenRequestedSessionIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestedSessionId("stale-session-id");
        request.setRequestedSessionIdValid(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("expired"));

        assertEquals(401, response.getStatus());
        assertEquals("SESSION_EXPIRED", JsonPath.read(response.getContentAsString(), "$.code"));
    }
}
