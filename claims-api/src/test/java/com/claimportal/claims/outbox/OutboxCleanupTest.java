package com.claimportal.claims.outbox;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxCleanupTest {

    private static final Instant NOW = Instant.parse("2026-10-24T08:30:00Z");

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final OutboxCleanup cleanup = new OutboxCleanup(repository, Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofDays(7));

    @Test
    void deletesPublishedEventsOlderThanTheRetentionPeriod() {
        Instant cutoff = Instant.parse("2026-10-17T08:30:00Z");
        when(repository.deletePublishedBefore(cutoff)).thenReturn(12);

        assertThat(cleanup.deleteOldPublishedEvents()).isEqualTo(12);

        verify(repository).deletePublishedBefore(cutoff);
    }
}
