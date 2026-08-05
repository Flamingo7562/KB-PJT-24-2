package com.gighub.auth.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.gighub.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

/** CSRF와 권한 검증에 실패한 요청에 공통 403 응답을 반환합니다. */
public final class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final String MESSAGE = "요청을 수행할 권한이 없습니다.";

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        SecurityErrorResponseWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                ApiErrorCode.FORBIDDEN,
                MESSAGE
        );
    }
}
