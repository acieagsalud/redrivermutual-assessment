package com.claimportal.claims.outbox;

import com.claimportal.claims.config.OutboxProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final Counter published;
    private final Counter publishFailures;

    public OutboxRelay(OutboxEventRepository repository, EventPublisher publisher, OutboxProperties properties, Clock clock,
                       MeterRegistry meterRegistry) {
        this.repository = repository;
        this.publisher = publisher;
        this.properties = properties;
        this.clock = clock;
        this.published = Counter.builder("outbox.published")
                .description("Outbox events confirmed by the broker")
                .register(meterRegistry);
        this.publishFailures = Counter.builder("outbox.publish.failures")
                .description("Failed attempts to publish an outbox event; the event stays queued for retry")
                .register(meterRegistry);
        // Rows waiting to be published. A value that keeps growing means the broker is unreachable.
        Gauge.builder("outbox.pending", repository, OutboxEventRepository::countByPublishedAtIsNull)
                .description("Outbox events not yet confirmed by the broker")
                .register(meterRegistry);
    }

    /** Publishes one batch and returns how many events were confirmed by the broker. */
    @Transactional
    public int publishPending() {
        List<OutboxEvent> batch = repository.lockUnpublished(properties.batchSize());
        int count = 0;
        for (OutboxEvent event : batch) {
            try {
                publisher.publish(event);
                event.markPublished(clock.instant());
                published.increment();
                count++;
            } catch (RuntimeException e) {
                event.recordFailure(e.getMessage());
                publishFailures.increment();
                log.warn("Outbox publish failed eventId={} attempts={}: {}", event.getId(), event.getAttempts(), e.getMessage());
                // The broker is most likely unavailable; stop and let the next poll retry the rest.
                break;
            }
        }
        if (count > 0) {
            log.debug("Outbox relayed {} event(s)", count);
        }
        return count;
    }
}
