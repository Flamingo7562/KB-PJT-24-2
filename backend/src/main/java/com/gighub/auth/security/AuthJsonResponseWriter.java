package com.gighub.auth.security;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gighub.auth.exception.AuthErrorCode;

/**
 * Security 필터 단계(EntryPoint/AccessDeniedHandler)에서 쓰는 JSON 오류 응답 헬퍼.
 * TODO(#116): 공통 오류 응답 DTO·직렬화가 병합되면 이 클래스를 제거하고 그 계약을 쓴다.
 */
public final class AuthJsonResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private AuthJsonResponseWriter() {
    }

    public static void write(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
        write(response, errorCode, errorCode.getDefaultMessage());
    }

    public static void write(HttpServletResponse response, AuthErrorCode errorCode, String message)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorCode.name());
        body.put("message", message);
        body.put("traceId", UUID.randomUUID().toString());

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(OBJECT_MAPPER.writeValueAsString(body));
    }
}
