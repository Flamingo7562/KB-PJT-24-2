package com.gighub.auth.validation;

import java.nio.charset.StandardCharsets;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class Utf8ByteLengthValidator implements ConstraintValidator<Utf8ByteLength, String> {

    private int max;

    @Override
    public void initialize(Utf8ByteLength annotation) {
        max = annotation.max();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
