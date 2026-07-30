package com.gighub.auth.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.gighub.auth.exception.AuthErrorCode;
import com.gighub.auth.exception.AuthException;
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
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new com.gighub.auth.exception.AuthExceptionHandler())
                .build();
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
}
