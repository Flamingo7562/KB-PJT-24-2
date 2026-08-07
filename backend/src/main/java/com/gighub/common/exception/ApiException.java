package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.api.ApiFieldError;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Objects;

/**
 * 도메인 계층이 HTTP 응답 계약을 공통 처리기에 전달하는 예외 기반 계약입니다.
 *
 * <p>공통 처리기는 구체적인 도메인 예외를 알지 않고 승인 상태·코드·안전한 메시지만 읽습니다.</p>
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ApiErrorCode code;
    private final List<ApiFieldError> fieldErrors;

    protected ApiException(HttpStatus status, ApiErrorCode code, String message) {
        this(status, code, message, null);
    }

    protected ApiException(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            List<ApiFieldError> fieldErrors) {
        super(Objects.requireNonNull(message, "message"));
        this.status = Objects.requireNonNull(status, "status");
        this.code = Objects.requireNonNull(code, "code");
        this.fieldErrors = fieldErrors == null ? null : List.copyOf(fieldErrors);
    }

}
