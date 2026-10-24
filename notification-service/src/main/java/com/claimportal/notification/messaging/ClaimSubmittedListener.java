package com.claimportal.notification.messaging;

import com.claimportal.notification.application.ClaimNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Adapts a broker message to a service call. No business logic lives here. */
@Component
public class ClaimSubmittedListener {

    static final String CORRELATION_HEADER = "X-Correlation-Id";
    static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(ClaimSubmittedListener.class);

    private final ClaimNotificationService service;
    private final ObjectMapper objectMapper;

    public ClaimSubmittedListener(ClaimNotificationService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @RabbitListener(queues = "${notification.queue.name}")
    public void onMessage(Message message) {
        Object correlationId = message.getMessageProperties().getHeader(CORRELATION_HEADER);
        if (correlationId != null) {
            MDC.put(MDC_KEY, correlationId.toString());
        }
        try {
            service.handle(parse(message));
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private ClaimSubmittedEvent parse(Message message) {
        try {
            ClaimSubmittedEvent event = objectMapper.readValue(message.getBody(), ClaimSubmittedEvent.class);
            if (event.eventId() == null || event.claimNumber() == null || event.customerEmail() == null) {
                throw new IllegalArgumentException("missing required fields");
            }
            return event;
        } catch (JacksonException | IllegalArgumentException e) {
            // Retrying cannot fix a malformed message: send it straight to the dead-letter queue.
            // Only the error type is logged: parser messages can quote field values such as the email address.
            log.error("Unreadable ClaimSubmitted message messageId={} error={}",
                    message.getMessageProperties().getMessageId(), e.getClass().getSimpleName());
            throw new AmqpRejectAndDontRequeueException("Unreadable ClaimSubmitted message");
        }
    }
}
