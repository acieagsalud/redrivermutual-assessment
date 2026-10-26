package com.claimportal.claims.outbox;

import com.claimportal.claims.config.OutboxProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2026-10-22T18:30:01Z");

    private final OutboxEventRepository repository = mock(OutboxEventRepository.class);
    private final EventPublisher publisher = mock(EventPublisher.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final OutboxRelay relay = new OutboxRelay(repository, publisher, new OutboxProperties(50), Clock.fixed(NOW, ZoneOffset.UTC), meters);

    @Test
    void marksEventPublishedOnlyAfterTheBrokerConfirms() {
        OutboxEvent event = event();
        when(repository.lockUnpublished(50)).thenReturn(List.of(event));

        assertThat(relay.publishPending()).isEqualTo(1);

        verify(publisher).publish(List.of(event));
        assertThat(event.isPublished()).isTrue();
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(meters.counter("outbox.published").count()).isEqualTo(1);
        assertThat(meters.counter("outbox.publish.failures").count()).isZero();
    }

    @Test
    void leavesEventUnpublishedAndRecordsTheErrorWhenPublishFails() {
        OutboxEvent event = event();
        when(repository.lockUnpublished(50)).thenReturn(List.of(event));
        doThrow(new AmqpException("Connection refused")).when(publisher).publish(List.of(event));

        assertThat(relay.publishPending()).isZero();

        assertThat(event.isPublished()).isFalse();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("Connection refused");
        assertThat(meters.counter("outbox.publish.failures").count()).isEqualTo(1);
        assertThat(meters.counter("outbox.published").count()).isZero();
    }

    @Test
    void reportsPendingEventsAsAGauge() {
        when(repository.countByPublishedAtIsNull()).thenReturn(3L);

        assertThat(meters.get("outbox.pending").gauge().value()).isEqualTo(3);
    }

    @Test
    void publishesTheWholeBatchWithOneConfirm() {
        List<OutboxEvent> batch = List.of(event(), event(), event());
        when(repository.lockUnpublished(50)).thenReturn(batch);

        assertThat(relay.publishPending()).isEqualTo(3);

        verify(publisher).publish(batch);
        assertThat(batch).allMatch(OutboxEvent::isPublished);
        assertThat(meters.counter("outbox.published").count()).isEqualTo(3);
    }

    @Test
    void aFailedConfirmLeavesTheWholeBatchForTheNextRun() {
        List<OutboxEvent> batch = List.of(event(), event());
        when(repository.lockUnpublished(50)).thenReturn(batch);
        doThrow(new AmqpException("Confirm timed out")).when(publisher).publish(batch);

        assertThat(relay.publishPending()).isZero();

        // Some messages may have reached the broker; republishing them is safe because the consumer dedupes.
        assertThat(batch).noneMatch(OutboxEvent::isPublished);
        assertThat(batch).allMatch(e -> e.getAttempts() == 1);
        assertThat(meters.counter("outbox.publish.failures").count()).isEqualTo(2);
    }

    @Test
    void reportsWhetherABatchWasFull() {
        assertThat(relay.wasFullBatch(50)).isTrue();
        assertThat(relay.wasFullBatch(49)).isFalse();
        assertThat(relay.wasFullBatch(0)).isFalse();
    }

    @Test
    void doesNothingWhenThereIsNothingToSend() {
        when(repository.lockUnpublished(50)).thenReturn(List.of());

        assertThat(relay.publishPending()).isZero();
        verify(publisher, never()).publish(any());
    }

    private static OutboxEvent event() {
        return new OutboxEvent(UUID.randomUUID(), UUID.randomUUID(), "ClaimSubmitted", "{}", "corr-1", NOW);
    }
}
