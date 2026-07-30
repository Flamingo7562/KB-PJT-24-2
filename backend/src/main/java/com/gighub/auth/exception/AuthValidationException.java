package com.gighub.auth.exception;

import java.util.List;

public class AuthValidationException extends AuthException {

    private static final long serialVersionUID = 1L;

    private final List<FieldErrorItem> fieldErrors;

    public AuthValidationException(List<FieldErrorItem> fieldErrors) {
        super(AuthErrorCode.VALIDATION_FAILED);
        this.fieldErrors = fieldErrors;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }
}
