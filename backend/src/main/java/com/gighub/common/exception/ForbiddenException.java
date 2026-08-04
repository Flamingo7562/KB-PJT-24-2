package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** 역할 또는 소유권 검증을 통과하지 못한 요청을 나타냅니다. */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, ApiErrorCode.FORBIDDEN, message);
    }
}
