package com.gighub.auth.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

import com.gighub.auth.exception.AuthErrorCode;

/**
 * 인가 거부 요청에 대해 JSON 403을 반환한다. CSRF 검증 실패는 CSRF_TOKEN_INVALID로
 * 구분하고, 그 외 접근 거부는 RESOURCE_FORBIDDEN이다.
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                        AccessDeniedException accessDeniedException) throws IOException {
        AuthErrorCode errorCode = accessDeniedException instanceof CsrfException
                ? AuthErrorCode.CSRF_TOKEN_INVALID
                : AuthErrorCode.RESOURCE_FORBIDDEN;
        AuthJsonResponseWriter.write(response, errorCode);
    }
}
