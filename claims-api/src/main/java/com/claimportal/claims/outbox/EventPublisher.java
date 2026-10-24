package com.claimportal.claims.outbox;

import java.util.List;

/**
 * Sends outbox events to the message broker. Returns only once the broker has confirmed all of them;
 * throws if any one is not confirmed, in which case none should be treated as sent.
 */
public interface EventPublisher {

    void publish(List<OutboxEvent> events);
}
