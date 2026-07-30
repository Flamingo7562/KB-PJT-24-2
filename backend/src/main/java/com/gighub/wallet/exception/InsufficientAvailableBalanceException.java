package com.gighub.wallet.exception;

// 출금 요청이 available_balance를 초과한 경우 -> 409 INSUFFICIENT_AVAILABLE_BALANCE
public class InsufficientAvailableBalanceException extends RuntimeException{
    public InsufficientAvailableBalanceException(String message){
        super(message);
    }
}
