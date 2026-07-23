package com.gighub.wallet.exception;

public class InsufficientWalletBalanceException extends RuntimeException{
    public InsufficientWalletBalanceException(String message) {
        super(message);
    }
}
