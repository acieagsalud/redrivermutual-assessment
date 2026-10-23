package com.claimportal.claims.event;

import com.claimportal.claims.domain.Claim;

import java.time.Instant;
import java.util.UUID;

/**
 * Version 1 of the ClaimSubmitted contract. Carries only what the confirmation email needs, so the
 * address, policy number and damage description never travel over the message broker.
 *
 * <p>The JSON shape is pinned by {@code contracts/claim-submitted.v1.json}, which both services test against.
 */
public record ClaimSubmittedEvent(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String claimNumber,
        String customerFirstName,
        String customerEmail) {

    public static final String TYPE = "ClaimSubmitted";
    public static final int CURRENT_VERSION = 1;

    public static ClaimSubmittedEvent from(Claim claim, UUID eventId, Instant occurredAt) {
        return new ClaimSubmittedEvent(
                eventId,
                TYPE,
                CURRENT_VERSION,
                occurredAt,
                claim.getClaimNumber(),
                claim.getFirstName(),
                claim.getEmail());
    }
}
