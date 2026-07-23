package com.gighub.wallet.exception;

public class InvalidEscrowStateException extends RuntimeException{
    public InvalidEscrowStateException(String message) {
        super(message);
    }
}
