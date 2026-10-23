package com.claimportal.claims.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.Clock;
import java.time.LocalDate;

/** Spring injects the clock, so tests can pin "today". */
public class LossDateValidator implements ConstraintValidator<ValidLossDate, LocalDate> {

    private final Clock clock;

    public LossDateValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        LocalDate today = LocalDate.now(clock);
        return !value.isAfter(today) && !value.isBefore(today.minusYears(1));
    }
}
