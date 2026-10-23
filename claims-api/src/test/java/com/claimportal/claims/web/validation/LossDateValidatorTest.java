package com.claimportal.claims.web.validation;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LossDateValidatorTest {

    private static final ZoneId WINNIPEG = ZoneId.of("America/Winnipeg");

    // 2026-10-22 at 20:00 in Winnipeg, which is already 2026-10-23 in UTC.
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-23T01:00:00Z"), WINNIPEG);
    private final LossDateValidator validator = new LossDateValidator(clock);

    @Test
    void acceptsToday() {
        assertThat(validator.isValid(LocalDate.of(2026, 10, 22), null)).isTrue();
    }

    @Test
    void rejectsTomorrowEvenThoughItIsAlreadyThatDateInUtc() {
        assertThat(validator.isValid(LocalDate.of(2026, 10, 23), null)).isFalse();
    }

    @Test
    void acceptsExactlyOneYearAgo() {
        assertThat(validator.isValid(LocalDate.of(2025, 10, 22), null)).isTrue();
    }

    @Test
    void rejectsMoreThanOneYearAgo() {
        assertThat(validator.isValid(LocalDate.of(2025, 10, 21), null)).isFalse();
    }

    @Test
    void leavesNullToNotNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
