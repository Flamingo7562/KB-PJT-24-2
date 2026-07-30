package com.gighub.auth.security;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.gighub.auth.exception.AuthErrorCode;

/**
 * 미인증·세션 만료 요청에 대해 JSON 401을 반환한다. 요청에 세션 id가 있었는데
 * 무효로 판명된 경우만 SESSION_EXPIRED로 구분하고, 그 외에는 AUTH_REQUIRED다.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        String requestedSessionId = request.getRequestedSessionId();
        boolean hadStaleSession = requestedSessionId != null && !request.isRequestedSessionIdValid();

        AuthErrorCode errorCode = hadStaleSession ? AuthErrorCode.SESSION_EXPIRED : AuthErrorCode.AUTH_REQUIRED;
        AuthJsonResponseWriter.write(response, errorCode);
    }
}
