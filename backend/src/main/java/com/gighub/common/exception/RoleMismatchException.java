package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;

/** 선택한 역할과 인증 사용자의 실제 역할이 다른 요청입니다. */
public class RoleMismatchException extends ApiException {

    public RoleMismatchException(String message) {
        super(HttpStatus.FORBIDDEN, ApiErrorCode.ROLE_MISMATCH, message);
    }
}
