package com.gighub.auth.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(SecurityConfig.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void exposesBcryptPasswordEncoder() {
        PasswordEncoder encoder = context.getBean(PasswordEncoder.class);

        assertInstanceOf(BCryptPasswordEncoder.class, encoder);
        String hash = encoder.encode("password1234");
        assertNotEquals("password1234", hash);
        assertTrue(encoder.matches("password1234", hash));
    }

    @Test
    void exposesSecurityFilterChain() {
        assertNotEquals(0, context.getBeansOfType(SecurityFilterChain.class).size());
    }
}
