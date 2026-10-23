package com.claimportal.claims.outbox;

import com.claimportal.claims.config.EventsProperties;
import com.claimportal.claims.observability.CorrelationId;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Publishes to RabbitMQ and blocks until the broker confirms the message is stored
 * (publisher confirms). Without the confirm, "sent" would only mean "handed to the client library".
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
    public void publish(OutboxEvent event) {
        Message message = toMessage(event);
        rabbitTemplate.invoke(ops -> {
            ops.send(properties.exchange(), properties.routingKey(), message);
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
