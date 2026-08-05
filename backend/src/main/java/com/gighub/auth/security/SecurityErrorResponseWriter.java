package com.gighub.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.api.ApiErrorResponse;
import com.gighub.common.trace.TraceIds;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Security Filter 단계의 오류를 승인된 공통 JSON Envelope로 기록합니다. */
final class SecurityErrorResponseWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private SecurityErrorResponseWriter() {
    }

    static void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            ApiErrorCode code,
            String message) throws IOException {
        ApiErrorResponse body = new ApiErrorResponse(
                code,
                message,
                TraceIds.getOrCreate(request),
                null
        );

        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }
}
