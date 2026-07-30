package com.gighub.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SessionAuthenticatorTest {

    private final SessionAuthenticator sessionAuthenticator =
            new SessionAuthenticator(CookieCsrfTokenRepository.withHttpOnlyFalse());

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsAuthenticationInSecurityContext() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new org.springframework.mock.web.MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthPrincipal principal = new AuthPrincipal(1L, "OWNER", "김사장");

        sessionAuthenticator.login(request, response, principal);

        assertEquals(principal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void rotatesSessionId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new org.springframework.mock.web.MockHttpSession());
        String originalId = request.getSession().getId();
        MockHttpServletResponse response = new MockHttpServletResponse();

        sessionAuthenticator.login(request, response, new AuthPrincipal(1L, "WORKER", "이알바"));

        assertNotEquals(originalId, request.getSession().getId());
    }

    @Test
    void rotatesCsrfCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new org.springframework.mock.web.MockHttpSession());
        MockHttpServletResponse response = new MockHttpServletResponse();

        sessionAuthenticator.login(request, response, new AuthPrincipal(1L, "WORKER", "이알바"));

        assertNotNull(response.getCookie("XSRF-TOKEN"));
    }
}
