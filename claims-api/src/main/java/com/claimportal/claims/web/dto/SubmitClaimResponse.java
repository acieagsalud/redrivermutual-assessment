package com.claimportal.claims.web.dto;

import java.time.Instant;

public record SubmitClaimResponse(String claimNumber, String status, Instant submittedAt) {
}
