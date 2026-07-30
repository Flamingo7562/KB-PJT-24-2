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

/**
 * 이슈 #71 완료조건 그대로: csrf → availability → signup → login → session → logout →
 * session 전 흐름이 실 MySQL 위에서 통과하는지 확인한다.
 */
@Tag("database")
class AuthFullFlowIntegrationTest {

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
    void csrfAvailabilitySignupLoginSessionLogoutSession() throws Exception {
        String loginId = "fullflow" + (System.currentTimeMillis() % 100000);
        String email = loginId + "@example.com";

        // 1) csrf
        MvcResult csrfResult = mockMvc.perform(get("/api/health")).andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
        String staleCsrfToken = csrfCookie.getValue();

        // 2) availability
        mockMvc.perform(get("/api/auth/login-id-availability").param("loginId", loginId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true));

        // 3) signup
        String signupBody = String.format(
                "{\"loginId\":\"%s\",\"password\":\"abcd1234\",\"passwordConfirm\":\"abcd1234\","
                        + "\"name\":\"풀플로우\",\"email\":\"%s\",\"role\":\"WORKER\"}",
                loginId, email);
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json").content(signupBody)
                        .header("X-XSRF-TOKEN", staleCsrfToken).cookie(csrfCookie))
                .andExpect(status().isCreated());

        // 4) login
        String loginBody = String.format(
                "{\"loginId\":\"%s\",\"password\":\"abcd1234\",\"expectedRole\":\"WORKER\"}", loginId);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json").content(loginBody)
                        .header("X-XSRF-TOKEN", staleCsrfToken).cookie(csrfCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("WORKER"))
                .andReturn();
        MockHttpSession sessionAfterLogin = (MockHttpSession) loginResult.getRequest().getSession(false);

        // 5) session (authenticated)
        mockMvc.perform(get("/api/auth/session").session(sessionAfterLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(true))
                .andExpect(jsonPath("$.data.name").value("풀플로우"));

        // 로그인 응답에서 회전된 새 CSRF 쿠키를 가져온다 (로그아웃 호출에 필요).
        Cookie csrfAfterLogin = loginResult.getResponse().getCookie("XSRF-TOKEN");

        // CSRF는 double-submit 쿠키 방식이라 헤더==쿠키(자기 자신과 일치)면 언제나 통과한다 —
        // "회전"의 실제 의미는 로그인 이후 예전 헤더 값이 새로 발급된 쿠키와 더는 일치하지
        // 않는다는 것이다. 예전 헤더 + 로그인 후 쿠키(불일치 조합)로 확인한다.
        mockMvc.perform(post("/api/work-cases/1/settlement/approve")
                        .session(sessionAfterLogin)
                        .header("Idempotency-Key", "mismatched-csrf-after-rotation")
                        .header("X-XSRF-TOKEN", staleCsrfToken).cookie(csrfAfterLogin))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));

        // 6) logout
        mockMvc.perform(post("/api/auth/logout")
                        .session(sessionAfterLogin)
                        .header("X-XSRF-TOKEN", csrfAfterLogin.getValue()).cookie(csrfAfterLogin))
                .andExpect(status().isOk());

        // 7) session (unauthenticated) — 같은 세션 객체라도 invalidate 되었으므로 미인증.
        mockMvc.perform(get("/api/auth/session").session(sessionAfterLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.authenticated").value(false));

        // 로그아웃 후: 세션이 없으니 (CSRF는 자기 자신과 일치해 통과해도) 보호 API는 401.
        MvcResult freshCsrfResult = mockMvc.perform(get("/api/health")).andReturn();
        Cookie freshCsrf = freshCsrfResult.getResponse().getCookie("XSRF-TOKEN");
        mockMvc.perform(post("/api/work-cases/1/settlement/approve")
                        .header("Idempotency-Key", "post-logout-no-session")
                        .header("X-XSRF-TOKEN", freshCsrf.getValue()).cookie(freshCsrf))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }
}
