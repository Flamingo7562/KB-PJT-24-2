package com.gighub.auth.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthErrorCodeTest {

    @Test
    void authRequiredMapsToUnauthorized() {
        assertEquals(HttpStatus.UNAUTHORIZED, AuthErrorCode.AUTH_REQUIRED.getHttpStatus());
        assertFalse(AuthErrorCode.AUTH_REQUIRED.getDefaultMessage().isBlank());
    }

    @Test
    void roleMismatchMapsToForbidden() {
        assertEquals(HttpStatus.FORBIDDEN, AuthErrorCode.ROLE_MISMATCH.getHttpStatus());
    }

    @Test
    void csrfTokenInvalidMapsToForbidden() {
        assertEquals(HttpStatus.FORBIDDEN, AuthErrorCode.CSRF_TOKEN_INVALID.getHttpStatus());
    }
}
