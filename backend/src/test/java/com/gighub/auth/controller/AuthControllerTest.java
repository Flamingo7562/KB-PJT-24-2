package com.gighub.auth.controller;

import java.util.List;

import com.gighub.auth.security.AuthPrincipal;
import com.gighub.auth.service.AuthService;
import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService)).build();
    }

    @Test
    void csrfReferencesDeferredTokenAndReturnsNoContent() throws Exception {
        DefaultCsrfToken token = new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", "token-value");

        mockMvc.perform(get("/api/auth/csrf")
                        .requestAttr(CsrfToken.class.getName(), token))
                .andExpect(status().isNoContent());
    }

    @Test
    void anonymousSessionReturnsOnlyAuthenticatedFalse() throws Exception {
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(false))
                .andExpect(jsonPath("$.data.role").doesNotExist())
                .andExpect(jsonPath("$.data.name").doesNotExist())
                .andExpect(jsonPath("$.data.needsWorkplaceSetup").doesNotExist());
    }

    @Test
    void authenticatedSessionReturnsCurrentRoleNameAndWorkplaceState() throws Exception {
        AuthPrincipal principal = new AuthPrincipal(7L, UserRole.OWNER, "김사장");
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
        when(authService.needsWorkplaceSetup(principal)).thenReturn(true);

        mockMvc.perform(get("/api/auth/session").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andExpect(jsonPath("$.data.name").value("김사장"))
                .andExpect(jsonPath("$.data.needsWorkplaceSetup").value(true));

        verify(authService).needsWorkplaceSetup(principal);
    }
}
