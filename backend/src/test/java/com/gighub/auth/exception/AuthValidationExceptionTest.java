package com.gighub.auth.exception;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthValidationExceptionTest {

    @Test
    void carriesFieldErrorsAndValidationFailedCode() {
        List<FieldErrorItem> errors = List.of(new FieldErrorItem("loginId", "형식이 올바르지 않습니다."));

        AuthValidationException exception = new AuthValidationException(errors);

        assertEquals(AuthErrorCode.VALIDATION_FAILED, exception.getErrorCode());
        assertEquals(errors, exception.getFieldErrors());
    }
}
