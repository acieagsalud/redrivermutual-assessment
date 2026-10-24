package com.claimportal.claims.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    /**
     * Locks the oldest unpublished events. SKIP LOCKED lets several API instances relay in parallel
     * without picking up the same row twice.
     */
    @Query(value = """
            SELECT * FROM outbox_event
            WHERE published_at IS NULL
            ORDER BY created_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockUnpublished(@Param("limit") int limit);

    long countByPublishedAtIsNull();

    /** Removes events the broker confirmed before the cutoff; unpublished rows are never touched. */
    @Modifying
    @Query(value = "DELETE FROM outbox_event WHERE published_at < :cutoff", nativeQuery = true)
    int deletePublishedBefore(@Param("cutoff") Instant cutoff);
}
