package com.gighub.auth.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gighub.auth.exception.AuthErrorCode;
import com.gighub.auth.exception.AuthException;
import com.gighub.auth.security.SessionAuthenticator;
import com.gighub.auth.service.AuthService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        SessionAuthenticator sessionAuthenticator =
                new SessionAuthenticator(CookieCsrfTokenRepository.withHttpOnlyFalse());
        AuthController controller = new AuthController(authService, sessionAuthenticator);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.gighub.auth.exception.AuthExceptionHandler())
                .build();
    }

    @AfterEach
    void tearDownSecurityContext() {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void loginIdAvailabilityReturnsWrappedBoolean() throws Exception {
        when(authService.isLoginIdAvailable("free")).thenReturn(true);

        mockMvc.perform(get("/api/auth/login-id-availability").param("loginId", "free"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    void emailAvailabilityReturnsWrappedBoolean() throws Exception {
        when(authService.isEmailAvailable("taken@test.com")).thenReturn(false);

        mockMvc.perform(get("/api/auth/email-availability").param("email", "taken@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    void signupReturns201WithUserId() throws Exception {
        when(authService.signup(org.mockito.ArgumentMatchers.any())).thenReturn(42L);

        String body = "{"
                + "\"loginId\":\"tester01\",\"password\":\"abcd1234\",\"passwordConfirm\":\"abcd1234\","
                + "\"name\":\"김테스트\",\"email\":\"tester01@example.com\",\"role\":\"WORKER\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").value(42));
    }

    @Test
    void signupReturns400WithFieldErrorsOnInvalidBody() throws Exception {
        String body = "{"
                + "\"loginId\":\"a\",\"password\":\"abcd1234\",\"passwordConfirm\":\"abcd1234\","
                + "\"name\":\"김테스트\",\"email\":\"tester01@example.com\",\"role\":\"WORKER\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("loginId"));
    }

    @Test
    void signupReturns409OnDuplicateLoginId() throws Exception {
        when(authService.signup(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new AuthException(AuthErrorCode.LOGIN_ID_ALREADY_EXISTS));

        String body = "{"
                + "\"loginId\":\"tester01\",\"password\":\"abcd1234\",\"passwordConfirm\":\"abcd1234\","
                + "\"name\":\"김테스트\",\"email\":\"tester01@example.com\",\"role\":\"WORKER\"}";

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
    }

    @Test
    void loginReturns200WithRoleNameAndWorkplaceFlag() throws Exception {
        when(authService.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new com.gighub.auth.security.AuthPrincipal(1L, "OWNER", "김사장"));
        when(authService.needsWorkplaceSetup(org.mockito.ArgumentMatchers.any())).thenReturn(true);

        String body = "{\"loginId\":\"tester01\",\"password\":\"abcd1234\",\"expectedRole\":\"OWNER\"}";

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andExpect(jsonPath("$.data.name").value("김사장"))
                .andExpect(jsonPath("$.data.needsWorkplaceSetup").value(true));
    }

    @Test
    void loginReturns401OnInvalidCredentials() throws Exception {
        when(authService.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        String body = "{\"loginId\":\"tester01\",\"password\":\"wrong\",\"expectedRole\":\"WORKER\"}";

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void loginReturns403OnRoleMismatch() throws Exception {
        when(authService.authenticate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new AuthException(AuthErrorCode.ROLE_MISMATCH));

        String body = "{\"loginId\":\"tester01\",\"password\":\"abcd1234\",\"expectedRole\":\"OWNER\"}";

        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_MISMATCH"));
    }

    @Test
    void sessionReturnsUnauthenticatedWhenNoAuthentication() throws Exception {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }

    @Test
    void sessionReturnsAuthenticatedDetailsWhenLoggedIn() throws Exception {
        when(authService.needsWorkplaceSetup(org.mockito.ArgumentMatchers.any())).thenReturn(false);
        org.springframework.security.core.context.SecurityContext context =
                org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                new com.gighub.auth.security.AuthPrincipal(1L, "WORKER", "이알바"), null, java.util.List.of()));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.role").value("WORKER"))
                .andExpect(jsonPath("$.data.name").value("이알바"));
    }
}
