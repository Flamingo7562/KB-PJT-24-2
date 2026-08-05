package com.gighub.auth.security;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import com.gighub.config.RootConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void exposesBcryptPasswordEncoder() {
        PasswordEncoder encoder = new SecurityConfig(new MockEnvironment()).passwordEncoder();

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        String hash = encoder.encode("password1234");
        assertTrue(encoder.matches("password1234", hash));
    }

    @Test
    void corsContractMatchesApprovedLocalBoundary() {
        CorsConfigurationSource source = new SecurityConfig(new MockEnvironment()).corsConfigurationSource();
        HttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        CorsConfiguration configuration = source.getCorsConfiguration(request);

        assertNotNull(configuration);
        assertEquals(Arrays.asList("http://localhost:5173"), configuration.getAllowedOrigins());
        assertEquals(Arrays.asList("Accept", "Content-Type", "X-XSRF-TOKEN", "Idempotency-Key"),
                configuration.getAllowedHeaders());
        assertEquals(Arrays.asList("Location", "Idempotency-Replayed"), configuration.getExposedHeaders());
        assertEquals(Boolean.TRUE, configuration.getAllowCredentials());
    }

    @Test
    void rootConfigImportsSecurityConfigExplicitly() {
        Import importConfig = RootConfig.class.getAnnotation(Import.class);

        assertNotNull(importConfig);
        assertTrue(Arrays.asList(importConfig.value()).contains(SecurityConfig.class));
    }
}
