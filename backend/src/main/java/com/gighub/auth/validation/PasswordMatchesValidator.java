package com.gighub.auth.validation;

import java.util.Objects;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.gighub.auth.dto.SignupRequest;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, SignupRequest> {

    @Override
    public boolean isValid(SignupRequest value, ConstraintValidatorContext context) {
        if (value == null || Objects.equals(value.getPassword(), value.getPasswordConfirm())) {
            return true;
        }

        // 객체 수준 비교 실패도 클라이언트가 수정할 필드로 정확히 반환합니다.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("passwordConfirm")
                .addConstraintViolation();
        return false;
    }
}
