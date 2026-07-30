package com.gighub.auth.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * 세션 인증 + CSRF 기반 Spring Security 설정.
 *
 * <p>{@code /api/auth/**}와 {@code /api/health/**}만 공개하고 나머지는 인증을
 * 요구한다. 이 설정 이후 지갑·에스크로 등 기존 API도 세션 쿠키 없이 호출하면
 * 401을 받는다 — 로그인 흐름(#71 후속 브랜치)이 준비되기 전까지는 통합 테스트
 * 관점에서 예상된 동작이다.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH_PATHS = {
        "/api/auth/csrf",
        "/api/auth/session",
        "/api/auth/login-id-availability",
        "/api/auth/email-availability",
        "/api/auth/signup",
        "/api/auth/login",
        "/api/auth/logout"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 10(BCryptPasswordEncoder 기본값). bcrypt는 72바이트를 넘는 입력을
        // 조용히 잘라버리므로, 회원가입 검증(AuthService, 후속 브랜치)에서 비밀번호
        // 최대 64자(멀티바이트 문자 고려한 안전 마진)를 상한으로 별도 강제한다.
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint() {
        return new JsonAuthenticationEntryPoint();
    }

    @Bean
    public JsonAccessDeniedHandler jsonAccessDeniedHandler() {
        return new JsonAccessDeniedHandler();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jsonAuthenticationEntryPoint())
                .accessDeniedHandler(jsonAccessDeniedHandler()))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PUBLIC_AUTH_PATHS).permitAll()
                .requestMatchers("/api/health/**").permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}
