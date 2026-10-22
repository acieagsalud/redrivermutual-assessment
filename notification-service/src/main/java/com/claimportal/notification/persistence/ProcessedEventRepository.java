package com.claimportal.notification.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Repository
public class ProcessedEventRepository {

    private final JdbcTemplate jdbc;

    public ProcessedEventRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Claims an event for processing. Returns false if it was already processed.
     *
     * <p>ON CONFLICT makes the check and the insert one atomic step. If two copies of the same event
     * arrive at once, the second waits on the first's row lock and then sees the conflict, so only
     * one of them sends the email.
     */
    public boolean markProcessed(UUID eventId, String claimNumber, Instant at) {
        int inserted = jdbc.update(
                "INSERT INTO processed_event (event_id, claim_number, processed_at) VALUES (?, ?, ?) ON CONFLICT (event_id) DO NOTHING",
                eventId, claimNumber, Timestamp.from(at));
        return inserted == 1;
    }
}
