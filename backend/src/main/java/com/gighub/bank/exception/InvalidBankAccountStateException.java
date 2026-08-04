package com.gighub.bank.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidBankAccountStateException extends ApiException {

    public InvalidBankAccountStateException(String message) {
        super(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, message);
    }
}
