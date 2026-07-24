package com.gighub.bank.exception;

public class BankTransferIntegrityException extends RuntimeException {

    public BankTransferIntegrityException(String message) {
        super(message);
    }
}
