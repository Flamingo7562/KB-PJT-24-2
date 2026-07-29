package com.gighub.wallet.exception;

public class WithdrawalIntegrityException extends RuntimeException{
    public WithdrawalIntegrityException(String message){
        super(message);
    }

    public WithdrawalIntegrityException(String message, Throwable cause){
        super(message, cause);
    }
}
