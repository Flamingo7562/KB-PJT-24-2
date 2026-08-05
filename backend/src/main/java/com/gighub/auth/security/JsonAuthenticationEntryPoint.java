package com.gighub.auth.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gighub.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 인증이 없거나 만료된 보호 요청에 공통 401 응답을 반환합니다. */
public final class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final String MESSAGE = "인증이 필요합니다.";

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        SecurityErrorResponseWriter.write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.AUTH_REQUIRED,
                MESSAGE
        );
    }
}
