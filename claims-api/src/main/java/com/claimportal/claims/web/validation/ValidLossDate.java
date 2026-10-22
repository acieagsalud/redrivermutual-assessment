package com.claimportal.claims.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Date of loss must be today or earlier, and no more than one year ago. Null is left to {@code @NotNull}. */
@Documented
@Constraint(validatedBy = LossDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLossDate {

    String message() default "must be today or within the past year";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
