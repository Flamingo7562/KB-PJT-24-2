package com.gighub.wallet.exception;

public class FundingIntegrityException extends RuntimeException {

    public FundingIntegrityException(String message) {
        super(message);
    }

    public FundingIntegrityException(String message, Throwable cause) {
        super(message, cause);
    }
}
