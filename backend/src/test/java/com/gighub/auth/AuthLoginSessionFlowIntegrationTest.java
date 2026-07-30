package com.gighub.auth;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.gighub.config.RootConfig;
import com.gighub.config.WebMvcConfig;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("database")
class AuthLoginSessionFlowIntegrationTest {

    private AnnotationConfigWebApplicationContext rootContext;
    private AnnotationConfigWebApplicationContext servletContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.register(RootConfig.class);
        rootContext.setServletContext(new MockServletContext());
        rootContext.refresh();

        servletContext = new AnnotationConfigWebApplicationContext();
        servletContext.setParent(rootContext);
        servletContext.register(WebMvcConfig.class);
        servletContext.setServletContext(rootContext.getServletContext());
        servletContext.refresh();

        mockMvc = MockMvcBuilders.webAppContextSetup(servletContext)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        servletContext.close();
        rootContext.close();
    }

    @Test
    void signupThenLoginThenSessionReflectsWorker() throws Exception {
        String loginId = "loginflow" + (System.currentTimeMillis() % 100000);
        String email = loginId + "@example.com";

        MvcResult csrfResult = mockMvc.perform(get("/api/health")).andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        String signupBody = String.format(
                "{\"loginId\":\"%s\",\"password\":\"abcd1234\",\"passwordConfirm\":\"abcd1234\","
                        + "\"name\":\"로그인테스트\",\"email\":\"%s\",\"role\":\"WORKER\"}",
                loginId, email);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json").content(signupBody)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()).cookie(csrfCookie))
                .andExpect(status().isCreated());

        // 잘못된 비밀번호 -> 401
        String wrongPasswordBody = String.format(
                "{\"loginId\":\"%s\",\"password\":\"wrongpass1\",\"expectedRole\":\"WORKER\"}", loginId);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json").content(wrongPasswordBody)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()).cookie(csrfCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

        // 역할 불일치 -> 403
        String wrongRoleBody = String.format(
                "{\"loginId\":\"%s\",\"password\":\"abcd1234\",\"expectedRole\":\"OWNER\"}", loginId);
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json").content(wrongRoleBody)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()).cookie(csrfCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ROLE_MISMATCH"));

        // 정상 로그인
        String loginBody = String.format(
                "{\"loginId\":\"%s\",\"password\":\"abcd1234\",\"expectedRole\":\"WORKER\"}", loginId);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json").content(loginBody)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue()).cookie(csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("WORKER"))
                .andExpect(jsonPath("$.data.name").value("로그인테스트"))
                .andExpect(jsonPath("$.data.needsWorkplaceSetup").value(false))
                .andReturn();

        // MockMvc는 Tomcat과 달리 세션 id 변경 시 JSESSIONID Set-Cookie를 자동으로 쓰지
        // 않는다 — 로그인 후 요청이 들고 있던 MockHttpSession 객체를 직접 재사용한다.
        MockHttpSession sessionAfterLogin = (MockHttpSession) loginResult.getRequest().getSession(false);

        // 로그인 세션으로 세션 조회
        mockMvc.perform(get("/api/auth/session").session(sessionAfterLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.role").value("WORKER"))
                .andExpect(jsonPath("$.data.name").value("로그인테스트"));

        // 세션 쿠키 없이 세션 조회 -> 미인증
        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(false));
    }
}
