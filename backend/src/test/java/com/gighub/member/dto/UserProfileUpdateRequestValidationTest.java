package com.gighub.member.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserProfileUpdateRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void normalizesPhoneBeforeValidation() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest("010-1234 5678");

        assertTrue(validator.validate(request).isEmpty());
        assertEquals("01012345678", request.getPhone());
    }

    @Test
    void treatsBlankPhoneAsAbsent() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(" - ");

        assertTrue(validator.validate(request).isEmpty());
        assertNull(request.getPhone());
    }

    @Test
    void allowsNullPhoneToClearStoredValue() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(null);

        assertTrue(validator.validate(request).isEmpty());
        assertNull(request.getPhone());
    }

    @Test
    void rejectsPhoneOutsideApprovedFormat() {
        assertTrue(hasPhoneViolation(new UserProfileUpdateRequest("123-456-789")));
        assertTrue(hasPhoneViolation(new UserProfileUpdateRequest("0101234")));
        assertTrue(hasPhoneViolation(new UserProfileUpdateRequest("010123456789")));
    }

    private boolean hasPhoneViolation(UserProfileUpdateRequest request) {
        Set<ConstraintViolation<UserProfileUpdateRequest>> violations = validator.validate(request);
        return violations.stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("phone"));
    }
}