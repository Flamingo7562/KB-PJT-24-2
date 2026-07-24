package com.gighub.bank.exception;

// 타인 소유 계좌이거나 존재하지 않는 계좌에 접근한 경우 -> 403
public class BankAccountForbiddenException extends RuntimeException{
    public BankAccountForbiddenException(String message){
        super(message);
    }
}
