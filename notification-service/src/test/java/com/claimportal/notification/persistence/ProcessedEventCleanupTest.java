package com.claimportal.notification.persistence;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProcessedEventCleanupTest {

    private static final Instant NOW = Instant.parse("2026-10-24T08:45:00Z");

    private final ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
    private final ProcessedEventCleanup cleanup = new ProcessedEventCleanup(repository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(30));

    @Test
    void forgetsEventsProcessedBeforeTheRetentionPeriod() {
        Instant cutoff = Instant.parse("2026-09-24T08:45:00Z");
        when(repository.deleteProcessedBefore(cutoff)).thenReturn(3);

        assertThat(cleanup.deleteOldEntries()).isEqualTo(3);

        verify(repository).deleteProcessedBefore(cutoff);
    }
}
