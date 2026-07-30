package com.gighub.auth.exception;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthExceptionHandlerTest {

    private final AuthExceptionHandler handler = new AuthExceptionHandler();

    @Test
    void mapsPlainAuthExceptionToItsErrorCodeStatus() {
        ResponseEntity<?> response =
                handler.handleAuthException(new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS));

        assertEquals(409, response.getStatusCodeValue());
    }

    @Test
    void mapsValidationExceptionWithFieldErrors() {
        AuthValidationException exception =
                new AuthValidationException(List.of(new FieldErrorItem("phone", "형식 오류")));

        ResponseEntity<?> response = handler.handleAuthException(exception);

        assertEquals(400, response.getStatusCodeValue());
    }
}
