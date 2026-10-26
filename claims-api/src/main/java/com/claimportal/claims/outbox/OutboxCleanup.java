package com.claimportal.claims.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;

/**
 * Keeps the outbox small. A published row is only useful for troubleshooting a recent delivery,
 * so rows older than the retention period are deleted once a night.
 */
@Component
public class OutboxCleanup {

    private static final Logger log = LoggerFactory.getLogger(OutboxCleanup.class);

    private final OutboxEventRepository repository;
    private final Clock clock;
    private final Duration retention;

    public OutboxCleanup(OutboxEventRepository repository, Clock clock,
                         @Value("${claims.outbox.retention:P7D}") Duration retention) {
        this.repository = repository;
        this.clock = clock;
        this.retention = retention;
    }

    @Scheduled(cron = "${claims.outbox.cleanup-cron:0 30 3 * * *}", zone = "${claims.time-zone}")
    @Transactional
    public int deleteOldPublishedEvents() {
        int deleted = repository.deletePublishedBefore(clock.instant().minus(retention));
        if (deleted > 0) {
            log.info("Outbox cleanup deleted {} published event(s) older than {}", deleted, retention);
        }
        return deleted;
    }
}
