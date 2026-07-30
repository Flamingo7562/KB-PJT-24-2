package com.gighub.auth.dto;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignupRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private SignupRequest validRequest() {
        SignupRequest request = new SignupRequest();
        request.setLoginId("tester01");
        request.setPassword("abcd1234");
        request.setPasswordConfirm("abcd1234");
        request.setName("김테스트");
        request.setEmail("tester01@example.com");
        request.setPhone("010-1234-5678");
        request.setRole("WORKER");
        return request;
    }

    @Test
    void validRequestHasNoViolations() {
        assertTrue(validator.validate(validRequest()).isEmpty());
    }

    @Test
    void rejectsShortLoginId() {
        SignupRequest request = validRequest();
        request.setLoginId("abc");

        Set<ConstraintViolation<SignupRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("loginId", violations.iterator().next().getPropertyPath().toString());
    }

    @Test
    void rejectsPasswordWithoutDigit() {
        SignupRequest request = validRequest();
        request.setPassword("abcdefgh");

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void rejectsInvalidEmail() {
        SignupRequest request = validRequest();
        request.setEmail("not-an-email");

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void rejectsInvalidRole() {
        SignupRequest request = validRequest();
        request.setRole("ADMIN");

        assertEquals(1, validator.validate(request).size());
    }

    @Test
    void allowsBlankPhoneSinceItIsOptional() {
        SignupRequest request = validRequest();
        request.setPhone(null);

        assertTrue(validator.validate(request).isEmpty());
    }
}
