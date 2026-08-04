package com.gighub.wallet.exception;

import com.gighub.common.exception.ValidationException;

public class InvalidFundingRequestException extends ValidationException {

    public InvalidFundingRequestException(String message) {
        super(message);
    }
}
