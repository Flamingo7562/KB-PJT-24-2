package com.gighub.common.trace;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

/** 모든 DispatcherServlet 요청에 하나의 추적 식별자를 부여합니다. */
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String MDC_KEY = "traceId";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String traceId = TraceIds.getOrCreate(request);
        MDC.put(MDC_KEY, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // Tomcat Thread Pool이 다음 요청에서 이전 사용자의 식별자를 재사용하지 않게 정리합니다.
            MDC.remove(MDC_KEY);
        }
    }
}
