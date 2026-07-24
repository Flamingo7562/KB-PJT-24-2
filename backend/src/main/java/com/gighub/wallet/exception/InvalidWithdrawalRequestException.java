package com.gighub.wallet.exception;

public class InvalidWithdrawalRequestException extends RuntimeException{
    public InvalidWithdrawalRequestException(String message){
        super(message);
    }
}
