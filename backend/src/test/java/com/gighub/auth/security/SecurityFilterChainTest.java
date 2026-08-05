package com.gighub.auth.security;

import javax.servlet.Filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityFilterChainTest {

    private AnnotationConfigWebApplicationContext rootContext;
    private AnnotationConfigWebApplicationContext servletContext;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        createContexts();
    }

    @AfterEach
    void tearDown() {
        closeContexts();
    }

    @Test
    void publicAndProtectedGetBoundariesUseRealFilterChain() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void stateChangingRequestWithoutCsrfUsesApprovedForbiddenEnvelope() throws Exception {
        mockMvc.perform(post("/api/protected"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType("application/json;charset=UTF-8"))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void corsPreflightAllowsOnlyApprovedLocalOriginAndCredentials() throws Exception {
        mockMvc.perform(options("/api/protected")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "X-XSRF-TOKEN"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));

        mockMvc.perform(options("/api/protected")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testLoginIsPublicOnlyInLocalProfile() throws Exception {
        mockMvc.perform(get("/api/test-login/1"))
                .andExpect(status().isUnauthorized());

        closeContexts();
        createContexts("local");

        mockMvc.perform(get("/api/test-login/1"))
                .andExpect(status().isOk());
    }

    private void createContexts(String... activeProfiles) {
        MockServletContext mockServletContext = new MockServletContext();

        rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.setServletContext(mockServletContext);
        rootContext.getEnvironment().setActiveProfiles(activeProfiles);
        rootContext.register(SecurityConfig.class);
        rootContext.refresh();

        servletContext = new AnnotationConfigWebApplicationContext();
        servletContext.setServletContext(mockServletContext);
        servletContext.setParent(rootContext);
        servletContext.register(TestWebConfig.class);
        servletContext.refresh();

        Filter securityFilter = rootContext.getBean("springSecurityFilterChain", Filter.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(servletContext)
                .addFilters(securityFilter)
                .build();
    }

    private void closeContexts() {
        if (servletContext != null) {
            servletContext.close();
            servletContext = null;
        }
        if (rootContext != null) {
            rootContext.close();
            rootContext = null;
        }
    }

    @Configuration
    @EnableWebMvc
    static class TestWebConfig {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping({"/api/health", "/api/protected", "/swagger-ui/index.html"})
        ResponseEntity<Void> getEndpoint() {
            return ResponseEntity.ok().build();
        }

        @PostMapping("/api/protected")
        ResponseEntity<Void> postEndpoint() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/api/test-login/{userId}")
        ResponseEntity<Long> testLogin(@PathVariable Long userId) {
            return ResponseEntity.ok(userId);
        }
    }
}
