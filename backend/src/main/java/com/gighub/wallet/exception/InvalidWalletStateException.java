package com.gighub.wallet.exception;

// 지갑이 없거나 상태 전이, 잔액 갱신이 불가능한 경우 -> 409
public class InvalidWalletStateException extends RuntimeException{
    public InvalidWalletStateException(String message){
        super(message);
    }
}
