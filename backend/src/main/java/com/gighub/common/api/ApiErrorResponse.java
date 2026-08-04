package com.gighub.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * 보호 명세의 {@code code/message/traceId/fieldErrors?} 오류 응답입니다.
 *
 * <p>{@code null}인 선택 필드는 {@link JsonInclude}로 JSON에서 생략합니다.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ApiErrorResponse {

    private final ApiErrorCode code;
    private final String message;
    private final String traceId;
    private final List<ApiFieldError> fieldErrors;

    public ApiErrorResponse(
            ApiErrorCode code,
            String message,
            String traceId,
            List<ApiFieldError> fieldErrors) {
        this.code = Objects.requireNonNull(code, "code");
        this.message = Objects.requireNonNull(message, "message");
        this.traceId = Objects.requireNonNull(traceId, "traceId");
        this.fieldErrors = fieldErrors == null ? null : List.copyOf(fieldErrors);
    }

}
