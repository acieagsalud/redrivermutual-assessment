package com.claimportal.claims.application;

import com.claimportal.claims.domain.ClaimStatus;

import java.time.Instant;

/** {@code replayed} is true when an earlier request with the same idempotency key created this claim. */
public record SubmittedClaim(String claimNumber, ClaimStatus status, Instant submittedAt, boolean replayed) {

    public SubmittedClaim(String claimNumber, ClaimStatus status, Instant submittedAt) {
        this(claimNumber, status, submittedAt, false);
    }
}
