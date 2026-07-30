package com.gighub.auth;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("database")
class AuthSignupFlowIntegrationTest {

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
    void availabilityThenSignupThenNoLongerAvailable() throws Exception {
        String loginId = "flow" + (System.currentTimeMillis() % 100000);
        String email = loginId + "@example.com";

        MvcResult csrfResult = mockMvc.perform(get("/api/auth/login-id-availability").param("loginId", loginId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andReturn();
        Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");

        String body = String.format(
                "{\"loginId\":\"%s\",\"password\":\"abcd1234\",\"passwordConfirm\":\"abcd1234\","
                        + "\"name\":\"통합테스트\",\"email\":\"%s\",\"role\":\"WORKER\"}",
                loginId, email);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(body)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .cookie(csrfCookie))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.userId").exists());

        mockMvc.perform(get("/api/auth/login-id-availability").param("loginId", loginId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));

        // 같은 아이디로 재가입 시도 → 409
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(body)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue())
                        .cookie(csrfCookie))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LOGIN_ID_ALREADY_EXISTS"));
    }
}
