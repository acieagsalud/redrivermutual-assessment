package com.claimportal.claims.outbox;

import com.claimportal.claims.config.OutboxProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

/**
 * Moves unpublished outbox events to the broker.
 *
 * <p>Delivery is at-least-once: if the broker confirms but this transaction then fails to commit,
 * the event is sent again on the next run. The consumer deduplicates on event id.
 */
@Component
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final EventPublisher publisher;
    private final OutboxProperties properties;
    private final Clock clock;

    public OutboxRelay(OutboxEventRepository repository, EventPublisher publisher, OutboxProperties properties, Clock clock) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
    }

    /** Publishes one batch and returns how many events were confirmed by the broker. */
    @Transactional
    public int publishPending() {
        List<OutboxEvent> batch = repository.lockUnpublished(properties.batchSize());
        int published = 0;
        for (OutboxEvent event : batch) {
            try {
                publisher.publish(event);
                event.markPublished(clock.instant());
                published++;
            } catch (RuntimeException e) {
                event.recordFailure(e.getMessage());
                log.warn("Outbox publish failed eventId={} attempts={}: {}", event.getId(), event.getAttempts(), e.getMessage());
                // The broker is most likely unavailable; stop and let the next poll retry the rest.
                break;
            }
        }
        if (published > 0) {
            log.debug("Outbox relayed {} event(s)", published);
        }
        return published;
    }
}
