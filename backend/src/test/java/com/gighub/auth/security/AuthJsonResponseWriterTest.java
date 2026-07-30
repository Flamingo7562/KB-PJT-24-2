package com.gighub.auth.security;

import com.gighub.auth.exception.AuthErrorCode;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthJsonResponseWriterTest {

    @Test
    void writesDefaultMessageAndStatus() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthJsonResponseWriter.write(response, AuthErrorCode.AUTH_REQUIRED);

        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        String body = response.getContentAsString();
        assertEquals("AUTH_REQUIRED", JsonPath.read(body, "$.code"));
        assertEquals(AuthErrorCode.AUTH_REQUIRED.getDefaultMessage(), JsonPath.read(body, "$.message"));
        assertFalse(((String) JsonPath.read(body, "$.traceId")).isBlank());
    }

    @Test
    void writesOverriddenMessage() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        AuthJsonResponseWriter.write(response, AuthErrorCode.ROLE_MISMATCH, "커스텀");

        assertEquals(403, response.getStatus());
        assertEquals("커스텀", JsonPath.read(response.getContentAsString(), "$.message"));
    }
}
