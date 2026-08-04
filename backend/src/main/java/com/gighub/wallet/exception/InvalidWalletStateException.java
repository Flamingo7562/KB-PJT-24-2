package com.gighub.wallet.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidWalletStateException extends ApiException {

    public InvalidWalletStateException(String message) {
        super(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, message);
    }
}
