package com.gighub.bank.exception;

// 계좌가 BLOCKED/CLOSED이거나 이체를 수행할 수 없는 상태인 경우 -> 409
public class InvalidBankAccountStateException extends RuntimeException {
    public InvalidBankAccountStateException(String message) {
        super(message);
    }
}
