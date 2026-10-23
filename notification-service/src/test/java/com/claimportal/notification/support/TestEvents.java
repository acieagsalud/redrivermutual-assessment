package com.claimportal.notification.support;

import com.claimportal.notification.messaging.ClaimSubmittedEvent;

import java.time.Instant;
import java.util.UUID;

public final class TestEvents {

    private TestEvents() {
    }

    public static ClaimSubmittedEvent event() {
        return event(UUID.randomUUID(), "Dana");
    }

    public static ClaimSubmittedEvent event(UUID eventId, String firstName) {
        return new ClaimSubmittedEvent(eventId, "ClaimSubmitted", 1, Instant.parse("2026-10-22T18:30:00Z"),
                "CLM-7K4Q-9XHM", firstName, "dana@example.com");
    }
}
