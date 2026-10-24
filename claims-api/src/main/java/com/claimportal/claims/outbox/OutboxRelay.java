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

    /**
     * Publishes one batch and returns how many events the broker confirmed: the whole batch or none.
     * If the confirm fails, some messages may still have reached the broker; they are sent again on
     * the next run and the consumer skips the copies.
     */
    @Transactional
    public int publishPending() {
        List<OutboxEvent> batch = repository.lockUnpublished(properties.batchSize());
        if (batch.isEmpty()) {
            return 0;
        }
        try {
            publisher.publish(batch);
        } catch (RuntimeException e) {
            batch.forEach(event -> event.recordFailure(e.getMessage()));
            publishFailures.increment(batch.size());
            log.warn("Outbox publish failed for {} event(s), first eventId={}: {}", batch.size(), batch.getFirst().getId(), e.getMessage());
            return 0;
        }
        batch.forEach(event -> event.markPublished(clock.instant()));
        published.increment(batch.size());
        log.debug("Outbox relayed {} event(s)", batch.size());
        return batch.size();
    }

    /** True when the last batch was full, so more events are probably waiting. */
    public boolean wasFullBatch(int published) {
        return published == properties.batchSize();
    }
}
