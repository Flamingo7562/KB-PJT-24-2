package com.gighub.common.api;

import java.util.Objects;

/** 입력 필드 하나의 검증 실패를 프런트 필드명과 안전한 사유로 전달합니다. */
public final class ApiFieldError {

    private final String field;
    private final String reason;

    public ApiFieldError(String field, String reason) {
        this.field = Objects.requireNonNull(field, "field");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    public String getField() {
        return field;
    }

    public String getReason() {
        return reason;
    }
}
