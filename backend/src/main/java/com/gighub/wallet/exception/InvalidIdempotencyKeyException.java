package com.gighub.wallet.exception;

import com.gighub.common.exception.ValidationException;

public class InvalidIdempotencyKeyException extends ValidationException {

    public InvalidIdempotencyKeyException(String message) {
        super(message);
    }
}
