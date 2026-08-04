package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** Controller 또는 Service가 발견한 안전한 입력 검증 실패를 나타냅니다. */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, ApiErrorCode.VALIDATION_ERROR, message);
    }
}
