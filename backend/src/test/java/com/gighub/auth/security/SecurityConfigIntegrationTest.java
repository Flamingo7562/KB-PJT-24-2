package com.gighub.auth.security;

import javax.servlet.http.Cookie;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실제 MySQL 위에서, 운영과 동일한 Root/Servlet Context 분리 구조로
 * SecurityConfig의 세션·CSRF·401 응답을 확인한다.
 *
 * <p>Root(RootConfig)와 Servlet(WebMvcConfig)를 하나로 합쳐 두면 SecurityConfig가
 * WebMvcConfig의 {@code mvcHandlerMappingIntrospector}를 우연히 볼 수 있어 실제
 * 배포(Root/Servlet 분리)에서만 나는 오류를 테스트가 놓친다 — 반드시 부모/자식으로
 * 나눠서 구성한다.</p>
 */
@Tag("database")
class SecurityConfigIntegrationTest {

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
    void publicHealthCheckIssuesReadableCsrfCookie() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void protectedEndpointWithoutCsrfTokenIsRejectedBeforeAuthCheck() throws Exception {
        // CSRF 필터가 인가 필터보다 먼저 실행되므로, 토큰이 아예 없으면 401이 아니라
        // 403 CSRF_TOKEN_INVALID(JsonAccessDeniedHandler)를 먼저 받는다.
        mockMvc.perform(post("/api/work-cases/1/settlement/approve")
                        .header("Idempotency-Key", "TEST-KEY"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
    }

    @Test
    void previouslyOpenWalletEndpointNowRequiresSessionOnceCsrfPasses() throws Exception {
        MvcResult csrfResult = mockMvc.perform(get("/api/health")).andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        mockMvc.perform(post("/api/work-cases/1/settlement/approve")
                        .header("Idempotency-Key", "TEST-KEY")
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .cookie(csrfCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void testLoginPathPassesThroughSecurityLayerWithoutAuth() throws Exception {
        // local 프로파일이 꺼져 있어 TestLoginController 빈 자체가 없으므로 404가 정상이다.
        // 여기서 확인하려는 건 그 이전 단계 — Security가 401/403으로 먼저 막지 않는지다.
        mockMvc.perform(get("/api/test-login/1"))
                .andExpect(status().isNotFound());
    }
}
