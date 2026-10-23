package com.claimportal.claims.application;

import com.claimportal.claims.domain.ClaimStatus;

import java.time.Instant;

public record SubmittedClaim(String claimNumber, ClaimStatus status, Instant submittedAt) {
}
