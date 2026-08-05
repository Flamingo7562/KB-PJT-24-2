package com.gighub.workplace.validation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import com.gighub.workplace.dto.WorkplaceCreateRequest;

public class CoordinatePairValidator
        implements ConstraintValidator<CoordinatePair, WorkplaceCreateRequest> {

    @Override
    public boolean isValid(WorkplaceCreateRequest value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean hasLatitude = value.getLatitude() != null;
        boolean hasLongitude = value.getLongitude() != null;
        if (hasLatitude == hasLongitude) {
            return true;
        }

        // 객체 수준 비교 실패도 클라이언트가 채워야 할 필드로 정확히 반환합니다.
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode(hasLatitude ? "longitude" : "latitude")
                .addConstraintViolation();
        return false;
    }
}
