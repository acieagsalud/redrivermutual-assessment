package com.claimportal.claims.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Triggers the relay on a fixed delay. Kept separate so the relay's transaction goes through the Spring proxy. */
@Component
public class OutboxPoller {

    private final OutboxRelay relay;

    public OutboxPoller(OutboxRelay relay) {
        this.relay = relay;
    }

    /**
     * Drains the backlog: keeps publishing while batches come back full, each batch in its own
     * transaction, so a burst of claims or the backlog after a broker outage clears at broker speed
     * rather than one batch per poll interval.
     */
    @Scheduled(fixedDelayString = "${claims.outbox.poll-interval-ms:1000}")
    public void poll() {
        int published;
        do {
            published = relay.publishPending();
        } while (relay.wasFullBatch(published));
    }
}
