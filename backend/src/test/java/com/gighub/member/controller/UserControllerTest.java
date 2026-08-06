package com.gighub.member.controller;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gighub.auth.security.AuthPrincipal;
import com.gighub.common.exception.CommonExceptionHandler;
import com.gighub.member.domain.User;
import com.gighub.member.domain.UserRole;
import com.gighub.member.domain.UserStatus;
import com.gighub.member.dto.UserProfileResponse;
import com.gighub.member.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private static final Long USER_ID = 42L;

    @Mock
    private UserService userService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new UserController(userService))
                .setControllerAdvice(new CommonExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .build();

        AuthPrincipal principal = new AuthPrincipal(USER_ID, UserRole.OWNER, "김사장");
        authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    @Test
    void returnsApprovedProfileFieldsOnly() throws Exception {
        when(userService.getProfile(USER_ID)).thenReturn(profile("01012345678"));

        mockMvc.perform(get("/api/users/me").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.loginId").value("owner01"))
                .andExpect(jsonPath("$.data.email").value("owner@example.com"))
                .andExpect(jsonPath("$.data.name").value("김사장"))
                .andExpect(jsonPath("$.data.phone").value("01012345678"))
                .andExpect(jsonPath("$.data.role").value("OWNER"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void rejectsProfileReadWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void updatesPhoneFromAuthenticatedPrincipalOnly() throws Exception {
        when(userService.updatePhone(eq(USER_ID), eq("01099998888")))
                .thenReturn(profile("01099998888"));

        mockMvc.perform(patch("/api/users/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"010-9999-8888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phone").value("01099998888"));
    }

    @Test
    void rejectsForbiddenProfileField() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"01012345678\",\"name\":\"해커\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(userService, never()).updatePhone(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsPhoneOutsideApprovedFormat() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"123-456-789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private UserProfileResponse profile(String phone) {
        User user = new User();
        user.setId(USER_ID);
        user.setLoginId("owner01");
        user.setEmail("owner@example.com");
        user.setName("김사장");
        user.setPhone(phone);
        user.setRole(UserRole.OWNER);
        user.setStatus(UserStatus.ACTIVE);
        return UserProfileResponse.from(user);
    }
}