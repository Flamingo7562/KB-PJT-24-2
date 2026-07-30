package com.gighub.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/**
 * Servlet 3.0+ 컨테이너가 시작 시 자동으로 발견해 {@code springSecurityFilterChain}
 * ({@link RootConfig}의 {@code SecurityConfig}가 만드는 필터체인)을 DispatcherServlet보다
 * 먼저 등록한다. 별도 설정이 필요 없다 — 클래스 존재 자체가 등록 신호다.
 */
public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {
}
