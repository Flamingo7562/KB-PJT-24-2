package com.gighub.wallet.exception;

import com.gighub.common.exception.ValidationException;

public class InvalidWithdrawalRequestException extends ValidationException {

    public InvalidWithdrawalRequestException(String message) {
        super(message);
    }
}
