package com.claimportal.notification.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

/**
 * This service's own view of ClaimSubmitted v1. It is deliberately not shared code with claims-api:
 * the two agree through {@code contracts/claim-submitted.v1.json}, and unknown fields are ignored so
 * the producer can add fields without breaking this consumer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClaimSubmittedEvent(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String claimNumber,
        String customerFirstName,
        String customerEmail) {
}
