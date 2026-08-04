package com.gighub.wallet.exception;

import com.gighub.common.api.ApiErrorCode;
import com.gighub.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class InsufficientWalletBalanceException extends ApiException {

    public InsufficientWalletBalanceException(String message) {
        super(HttpStatus.CONFLICT, ApiErrorCode.CONFLICT, message);
    }
}
