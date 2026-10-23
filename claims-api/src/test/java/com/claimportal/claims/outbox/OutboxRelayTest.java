package com.claimportal.claims.outbox;

import com.claimportal.claims.config.OutboxProperties;
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
    private final OutboxRelay relay = new OutboxRelay(repository, publisher, new OutboxProperties(50), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void marksEventPublishedOnlyAfterTheBrokerConfirms() {
        OutboxEvent event = event();
        when(repository.lockUnpublished(50)).thenReturn(List.of(event));

        assertThat(relay.publishPending()).isEqualTo(1);

        verify(publisher).publish(event);
        assertThat(event.isPublished()).isTrue();
        assertThat(event.getPublishedAt()).isEqualTo(NOW);
        assertThat(event.getAttempts()).isEqualTo(1);
    }

    @Test
    void leavesEventUnpublishedAndRecordsTheErrorWhenPublishFails() {
        OutboxEvent event = event();
        when(repository.lockUnpublished(50)).thenReturn(List.of(event));
        doThrow(new AmqpException("Connection refused")).when(publisher).publish(event);

        assertThat(relay.publishPending()).isZero();

        assertThat(event.isPublished()).isFalse();
        assertThat(event.getAttempts()).isEqualTo(1);
        assertThat(event.getLastError()).contains("Connection refused");
    }

    @Test
    void stopsTheBatchAtTheFirstFailureSoTheBrokerIsNotHammered() {
        OutboxEvent first = event();
        OutboxEvent second = event();
        when(repository.lockUnpublished(50)).thenReturn(List.of(first, second));
        doThrow(new AmqpException("Connection refused")).when(publisher).publish(first);

        relay.publishPending();

        verify(publisher, never()).publish(second);
        assertThat(second.getAttempts()).isZero();
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
