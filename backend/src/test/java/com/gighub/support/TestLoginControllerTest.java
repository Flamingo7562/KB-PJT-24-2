package com.gighub.support;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.security.AuthSessionManager;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestLoginControllerTest {

    @Test
    void localTestLoginUsesSameSecuritySessionBridge() throws Exception {
        TestLoginService testLoginService = mock(TestLoginService.class);
        AuthSessionManager authSessionManager = mock(AuthSessionManager.class);
        AuthPrincipal principal = new AuthPrincipal(91L, UserRole.WORKER, "로컬 사용자");
        when(testLoginService.loadActivePrincipal(91L)).thenReturn(principal);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new TestLoginController(testLoginService, authSessionManager)
        ).build();

        mockMvc.perform(get("/api/test-login/91"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginUserId").value(91));

        verify(authSessionManager).establish(any(), any(), eq(principal));
    }
}
