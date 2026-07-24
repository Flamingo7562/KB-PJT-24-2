package com.gighub.bank.exception;

// 연결 계좌의 available_amount가 요청 금액보다 적은 경우 -> 409
public class InsufficientBankBalanceException extends RuntimeException{
    public InsufficientBankBalanceException(String message){
        super(message);
    }
}
