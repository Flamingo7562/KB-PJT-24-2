package com.gighub.common.exception;

public class AuthRequiredException extends RuntimeException{
    public AuthRequiredException(String message){
        super(message);
    }
}
