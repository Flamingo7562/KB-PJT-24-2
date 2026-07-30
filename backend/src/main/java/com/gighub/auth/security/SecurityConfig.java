package com.gighub.auth.security;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * 세션 인증 + CSRF 기반 Spring Security 설정.
 *
 * <p>{@code /api/auth/**}와 {@code /api/health/**}만 공개하고 나머지는 인증을
 * 요구한다. 이 설정 이후 지갑·에스크로 등 기존 API도 세션 쿠키 없이 호출하면
 * 401을 받는다 — 로그인 흐름(#71 후속 브랜치)이 준비되기 전까지는 통합 테스트
 * 관점에서 예상된 동작이다.</p>
 *
 * <p>{@code /api/test-login/**}({@link com.gighub.support.TestLoginController},
 * {@code @Profile("local")})도 공개한다 — local 프로파일 밖에서는 컨트롤러 빈 자체가
 * 없어 경로가 존재하지 않으므로 다른 환경에는 영향이 없다.</p>
 *
 * <p>경로 매칭은 문자열 {@code requestMatchers(String...)} 대신 명시적
 * {@link AntPathRequestMatcher}를 쓴다. 문자열 오버로드는 Spring MVC가
 * classpath에 있으면 {@code mvcHandlerMappingIntrospector} 빈이 필요한
 * {@code MvcRequestMatcher}를 시도하는데, 이 앱은 Root Context({@link
 * com.gighub.config.RootConfig}, 이 클래스가 속한 곳)와 Servlet Context({@link
 * com.gighub.config.WebMvcConfig}, {@code @EnableWebMvc}가 있는 곳)가 분리되어 있어
 * 그 빈을 찾지 못하고 컨텍스트 시작이 실패한다.</p>
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

    private static RequestMatcher[] antMatchers(String... patterns) {
        return Arrays.stream(patterns)
                .map(AntPathRequestMatcher::new)
                .toArray(RequestMatcher[]::new);
    }

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
                .requestMatchers(antMatchers(PUBLIC_AUTH_PATHS)).permitAll()
                .requestMatchers(antMatchers("/api/health/**")).permitAll()
                .requestMatchers(antMatchers("/api/test-login/**")).permitAll()
                .anyRequest().authenticated());

        return http.build();
    }
}
