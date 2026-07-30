package com.gighub.auth.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonAccessDeniedHandlerTest {

    private final JsonAccessDeniedHandler handler = new JsonAccessDeniedHandler();

    @Test
    void returnsCsrfTokenInvalidForCsrfExceptions() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new MissingCsrfTokenException("token"));

        assertEquals(403, response.getStatus());
        assertEquals("CSRF_TOKEN_INVALID", JsonPath.read(response.getContentAsString(), "$.code"));
    }

    @Test
    void returnsResourceForbiddenForOtherAccessDeniedExceptions() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertEquals(403, response.getStatus());
        assertEquals("RESOURCE_FORBIDDEN", JsonPath.read(response.getContentAsString(), "$.code"));
    }
}
