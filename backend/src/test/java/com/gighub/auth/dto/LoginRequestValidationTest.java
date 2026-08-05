package com.gighub.auth.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesOnlyLoginId() {
        LoginRequest request = validRequest();
        request.setLoginId(" OWNER01 ");
        request.setPassword(" secret123 ");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("owner01", request.getLoginId());
        assertEquals(" secret123 ", request.getPassword());
    }

    @Test
    void rejectsPasswordPastBcryptUtf8Boundary() {
        LoginRequest request = validRequest();
        request.setPassword("가".repeat(25));

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("password")
        ));
    }

    @Test
    void rejectsLoginIdPastDatabaseLength() {
        LoginRequest request = validRequest();
        request.setLoginId("a".repeat(51));

        assertTrue(validator.validate(request).stream().anyMatch(
                violation -> violation.getPropertyPath().toString().equals("loginId")
        ));
    }

    private LoginRequest validRequest() {
        LoginRequest request = new LoginRequest();
        request.setLoginId("owner01");
        request.setPassword("secret123");
        request.setExpectedRole(UserRole.OWNER);
        return request;
    }
}
