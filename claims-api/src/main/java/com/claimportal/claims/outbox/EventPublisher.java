package com.claimportal.claims.outbox;

/** Sends an outbox event to the message broker. Returns only once the broker has confirmed it. */
public interface EventPublisher {

    void publish(OutboxEvent event);
}
