package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.api.ApiFieldError;
import org.springframework.http.HttpStatus;

import java.util.List;

/** Controller 또는 Service가 발견한 안전한 입력 검증 실패를 나타냅니다. */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message);
    }

    /** 프로그램 검증에서 발견한 필드 오류를 공통 오류 Envelope에 함께 전달합니다. */
    public ValidationException(String message, String field, String reason) {
        super(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.VALIDATION_ERROR,
                message,
                List.of(new ApiFieldError(field, reason))
        );
    }
}
