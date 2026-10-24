package com.claimportal.notification.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;

/** Keeps the deduplication table bounded by deleting old rows once a night. */
@Component
public class ProcessedEventCleanup {

    private static final Logger log = LoggerFactory.getLogger(ProcessedEventCleanup.class);

    private final ProcessedEventRepository repository;
    private final Clock clock;
    private final Duration retention;

    public ProcessedEventCleanup(ProcessedEventRepository repository, Clock clock,
                                 @Value("${notification.processed-event.retention:P30D}") Duration retention) {
        this.repository = repository;
        this.clock = clock;
        this.retention = retention;
    }

    @Scheduled(cron = "${notification.processed-event.cleanup-cron:0 45 3 * * *}", zone = "America/Winnipeg")
    public int deleteOldEntries() {
        int deleted = repository.deleteProcessedBefore(clock.instant().minus(retention));
        if (deleted > 0) {
            log.info("Deleted {} processed event(s) older than {}", deleted, retention);
        }
        return deleted;
    }
}
