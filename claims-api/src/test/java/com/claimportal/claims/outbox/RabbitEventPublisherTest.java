package com.claimportal.claims.outbox;

import com.claimportal.claims.config.EventsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RabbitEventPublisherTest {

    private final RabbitEventPublisher publisher = new RabbitEventPublisher(mock(RabbitTemplate.class),
            new EventsProperties("claims.events", "claim.submitted", Duration.ofSeconds(5)));

    @Test
    void messageCarriesEventIdCorrelationIdAndIsPersistent() {
        UUID eventId = UUID.randomUUID();
        OutboxEvent event = new OutboxEvent(eventId, UUID.randomUUID(), "ClaimSubmitted", "{\"a\":1}", "corr-123", Instant.now());

        Message message = publisher.toMessage(event);

        // The consumer deduplicates on messageId, so it must be the stable event id, never a fresh one.
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(eventId.toString());
        assertThat((String) message.getMessageProperties().getHeader("X-Correlation-Id")).isEqualTo("corr-123");
        assertThat((String) message.getMessageProperties().getHeader("event-type")).isEqualTo("ClaimSubmitted");
        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(new String(message.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"a\":1}");
    }
}
