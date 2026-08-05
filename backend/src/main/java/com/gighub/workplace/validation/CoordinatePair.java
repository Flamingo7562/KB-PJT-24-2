package com.gighub.workplace.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;
import javax.validation.Payload;

/** 위도와 경도를 모두 보내거나 모두 생략하도록 강제합니다. */
@Documented
@Constraint(validatedBy = CoordinatePairValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CoordinatePair {

    String message() default "위도와 경도는 함께 보내거나 모두 생략해야 합니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
