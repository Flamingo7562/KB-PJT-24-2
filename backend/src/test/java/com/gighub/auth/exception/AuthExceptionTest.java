package com.gighub.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthExceptionTest {

    @Test
    void usesDefaultMessageWhenNoneGiven() {
        AuthException exception = new AuthException(AuthErrorCode.INVALID_CREDENTIALS);

        assertEquals(AuthErrorCode.INVALID_CREDENTIALS, exception.getErrorCode());
        assertEquals(AuthErrorCode.INVALID_CREDENTIALS.getDefaultMessage(), exception.getMessage());
    }

    @Test
    void allowsCustomMessage() {
        AuthException exception = new AuthException(AuthErrorCode.ROLE_MISMATCH, "커스텀 메시지");

        assertEquals("커스텀 메시지", exception.getMessage());
    }
}
