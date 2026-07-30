package com.gighub.auth.exception;

/**
 * TODO(#116): 공통 ApplicationException이 병합되면 이를 상속하도록 교체한다.
 */
public class AuthException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final AuthErrorCode errorCode;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public AuthException(AuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}
