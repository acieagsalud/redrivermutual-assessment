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

    @Scheduled(fixedDelayString = "${claims.outbox.poll-interval-ms:1000}")
    public void poll() {
        relay.publishPending();
    }
}
