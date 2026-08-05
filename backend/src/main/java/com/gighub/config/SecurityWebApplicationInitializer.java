package com.gighub.config;

import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;

/** Tomcat이 DispatcherServlet보다 먼저 {@code springSecurityFilterChain}을 등록하게 합니다. */
public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {
}
