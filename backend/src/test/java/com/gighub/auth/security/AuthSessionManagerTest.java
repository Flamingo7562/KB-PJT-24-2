package com.gighub.auth.security;

import java.util.Arrays;

import javax.servlet.http.Cookie;

import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

class AuthSessionManagerTest {

    private final CsrfTokenRepository csrfTokenRepository = mock(CsrfTokenRepository.class);
    private final AuthSessionManager manager = new AuthSessionManager(csrfTokenRepository);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void establishCreatesSecurityContextAndLegacyBridgeWithoutExistingSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthPrincipal principal = new AuthPrincipal(61L, UserRole.WORKER, "김근로");

        manager.establish(request, response, principal);

        MockHttpSession session = (MockHttpSession) request.getSession(false);
        assertNotNull(session);
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
        );
        assertEquals(principal, context.getAuthentication().getPrincipal());
        assertEquals("ROLE_WORKER", context.getAuthentication().getAuthorities()
                .iterator().next().getAuthority());
        assertEquals(61L, session.getAttribute(AuthSessionManager.LEGACY_LOGIN_USER));
        verify(csrfTokenRepository).saveToken(isNull(), eq(request), eq(response));
    }

    @Test
    void establishRotatesExistingAnonymousSessionId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = (MockHttpSession) request.getSession(true);
        String previousSessionId = session.getId();

        manager.establish(
                request,
                new MockHttpServletResponse(),
                new AuthPrincipal(62L, UserRole.OWNER, "김사장")
        );

        assertNotEquals(previousSessionId, request.getSession(false).getId());
    }

    @Test
    void logoutInvalidatesSessionClearsContextAndExpiresCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthPrincipal principal = new AuthPrincipal(63L, UserRole.OWNER, "김사장");
        manager.establish(request, response, principal);
        MockHttpSession session = (MockHttpSession) request.getSession(false);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        response = new MockHttpServletResponse();
        clearInvocations(csrfTokenRepository);

        manager.logout(request, response, authentication);

        assertTrue(session.isInvalid());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        Cookie sessionCookie = Arrays.stream(response.getCookies())
                .filter(cookie -> cookie.getName().equals("JSESSIONID"))
                .findFirst()
                .orElseThrow();
        assertEquals(0, sessionCookie.getMaxAge());
        assertEquals("/", sessionCookie.getPath());
        assertTrue(sessionCookie.isHttpOnly());
        assertFalse(sessionCookie.getSecure());
        verify(csrfTokenRepository).saveToken(isNull(), eq(request), eq(response));
    }
}
