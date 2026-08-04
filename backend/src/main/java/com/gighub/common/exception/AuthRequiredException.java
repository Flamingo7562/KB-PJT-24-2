package com.gighub.common.exception;

import com.gighub.common.api.ApiErrorCode;
import org.springframework.http.HttpStatus;

public class AuthRequiredException extends ApiException {

    public AuthRequiredException(String message) {
        super(HttpStatus.UNAUTHORIZED, ApiErrorCode.AUTH_REQUIRED, message);
    }
}
