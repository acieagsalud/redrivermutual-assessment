package com.claimportal.claims.outbox;

import com.claimportal.claims.config.EventsProperties;
import com.claimportal.claims.observability.CorrelationId;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Publishes to RabbitMQ and blocks until the broker confirms the messages are stored
 * (publisher confirms). Without the confirm, "sent" would only mean "handed to the client library".
 *
 * <p>A batch is sent on one channel and confirmed with a single wait, so the round trip to the broker
 * is paid once per batch rather than once per message.
 */
@Component
public class RabbitEventPublisher implements EventPublisher {

    static final String EVENT_TYPE_HEADER = "event-type";

    private final RabbitTemplate rabbitTemplate;
    private final EventsProperties properties;

    public RabbitEventPublisher(RabbitTemplate rabbitTemplate, EventsProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    @Override
    public void publish(List<OutboxEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        rabbitTemplate.invoke(ops -> {
            for (OutboxEvent event : events) {
                ops.send(properties.exchange(), properties.routingKey(), toMessage(event));
            }
            ops.waitForConfirmsOrDie(properties.confirmTimeout().toMillis());
            return null;
        });
    }

    Message toMessage(OutboxEvent event) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setContentEncoding(StandardCharsets.UTF_8.name());
        props.setMessageId(event.getId().toString());
        props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        props.setHeader(EVENT_TYPE_HEADER, event.getEventType());
        if (event.getCorrelationId() != null) {
            props.setHeader(CorrelationId.HEADER, event.getCorrelationId());
        }
        return new Message(event.getPayload().getBytes(StandardCharsets.UTF_8), props);
    }
}
