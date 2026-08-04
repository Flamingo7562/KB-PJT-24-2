package com.gighub.wallet.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class IdempotencyKeyReusedException extends ApiException {

    public IdempotencyKeyReusedException(String message) {
        super(HttpStatus.CONFLICT, ApiErrorCode.IDEMPOTENCY_KEY_REUSED, message);
    }
}
