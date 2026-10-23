package com.claimportal.claims.outbox;

import com.claimportal.claims.event.ClaimSubmittedEvent;
import com.claimportal.claims.observability.CorrelationId;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

/** Records events in the outbox table as part of the caller's business transaction. */
@Component
public class OutboxWriter {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxWriter(OutboxEventRepository repository, ObjectMapper objectMapper, Clock clock) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * MANDATORY propagation: writing an outbox row outside the business transaction would defeat
     * the pattern, so this fails fast instead of silently starting a transaction of its own.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void append(UUID aggregateId, ClaimSubmittedEvent event) {
        String payload = objectMapper.writeValueAsString(event);
        repository.save(new OutboxEvent(
                event.eventId(), aggregateId, event.eventType(), payload, CorrelationId.current(), clock.instant()));
    }
}
