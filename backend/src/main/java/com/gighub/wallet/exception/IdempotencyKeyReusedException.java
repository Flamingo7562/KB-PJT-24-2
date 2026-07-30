package com.gighub.wallet.exception;

// 같은 멱등 키로 다른 요청 본문이 접수된 경우 -> 409
public class IdempotencyKeyReusedException extends RuntimeException{
    public IdempotencyKeyReusedException(String message){
        super(message);
    }
}
