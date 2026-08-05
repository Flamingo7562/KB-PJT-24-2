package com.gighub.auth.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.gighub.member.domain.UserRole;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignupRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesApprovedFieldsBeforeValidation() {
        SignupRequest request = validRequest();
        request.setLoginId("  USER  ");
        request.setEmail("  NOT-AN-EMAIL  ");
        request.setName("  김근로  ");
        request.setPhone("010-1234 5678");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("user", request.getLoginId());
        assertEquals("not-an-email", request.getEmail());
        assertEquals("김근로", request.getName());
        assertEquals("01012345678", request.getPhone());
    }

    @Test
    void acceptsIdentityMaximumLengthsWithoutExtraFormatRules() {
        SignupRequest request = validRequest();
        request.setLoginId("a".repeat(50));
        request.setEmail("b".repeat(255));

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void rejectsIdentityValuesPastDatabaseLengths() {
        SignupRequest request = validRequest();
        request.setLoginId("a".repeat(51));
        request.setEmail("b".repeat(256));

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "loginId"));
        assertTrue(hasViolation(violations, "email"));
    }

    @Test
    void rejectsPasswordPastBcryptUtf8BoundaryWithoutTruncation() {
        SignupRequest request = validRequest();
        request.setPassword("가".repeat(25));
        request.setPasswordConfirm("가".repeat(25));

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "password"));
        assertTrue(hasViolation(violations, "passwordConfirm"));
    }

    @Test
    void reportsPasswordMismatchOnConfirmationField() {
        SignupRequest request = validRequest();
        request.setPasswordConfirm("different-password");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertTrue(hasViolation(violations, "passwordConfirm"));
        assertFalse(hasViolation(violations, "password"));
    }

    @Test
    void treatsBlankOptionalPhoneAsAbsentAndRejectsInvalidPhone() {
        SignupRequest request = validRequest();
        request.setPhone(" - ");
        assertTrue(validator.validate(request).isEmpty());
        assertNull(request.getPhone());

        request.setPhone("123-456-789");
        assertTrue(hasViolation(validator.validate(request), "phone"));
    }

    private SignupRequest validRequest() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("worker01");
        request.setPassword("secret123");
        request.setPasswordConfirm("secret123");
        request.setName("김근로");
        request.setEmail("worker@example.com");
        request.setPhone("01012345678");
        request.setRole(UserRole.WORKER);
        return request;
    }

    private boolean hasViolation(
            Set<ConstraintViolation<SignupRequest>> violations,
            String field) {
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals(field));
    }
}
