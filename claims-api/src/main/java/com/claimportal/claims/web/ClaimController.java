package com.claimportal.claims.web;

import com.claimportal.claims.application.ClaimSubmissionService;
import com.claimportal.claims.application.NewClaim;
import com.claimportal.claims.application.SubmittedClaim;
import com.claimportal.claims.domain.Address;
import com.claimportal.claims.web.dto.AddressRequest;
import com.claimportal.claims.web.dto.SubmitClaimRequest;
import com.claimportal.claims.web.dto.SubmitClaimResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final Pattern IDEMPOTENCY_KEY = Pattern.compile("[A-Za-z0-9-]{1,64}");

    private final ClaimSubmissionService submissionService;
    private final Counter claimsSubmitted;

    public ClaimController(ClaimSubmissionService submissionService, MeterRegistry meterRegistry) {
        this.submissionService = submissionService;
        // Counted here, after the service's transaction has committed, so rolled-back submissions don't count.
        this.claimsSubmitted = Counter.builder("claims.submitted")
                .description("Claims saved with their ClaimSubmitted event queued in the outbox")
                .register(meterRegistry);
    }

    /**
     * 201 means the claim is saved and its confirmation email is queued. The email itself is sent
     * asynchronously by notification-service, so the client tells the customer to check their inbox.
     *
     * <p>An optional Idempotency-Key makes retries safe: a repeat of a key returns the claim the first
     * request created, with the same 201 body, instead of creating a second claim and a second email.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitClaimResponse submit(@Valid @RequestBody SubmitClaimRequest request,
                                      @RequestHeader(name = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        if (idempotencyKey != null && !IDEMPOTENCY_KEY.matcher(idempotencyKey).matches()) {
            throw new InvalidIdempotencyKeyException();
        }
        SubmittedClaim result;
        try {
            result = submissionService.submit(toNewClaim(request), idempotencyKey);
        } catch (DataIntegrityViolationException e) {
            // A concurrent request with the same key committed first; return its claim.
            if (idempotencyKey == null) {
                throw e;
            }
            result = submissionService.findExisting(idempotencyKey).orElseThrow(() -> e);
        }
        if (!result.replayed()) {
            claimsSubmitted.increment();
        }
        return new SubmitClaimResponse(result.claimNumber(), result.status().name(), result.submittedAt());
    }

    private static NewClaim toNewClaim(SubmitClaimRequest r) {
        AddressRequest a = r.address();
        return new NewClaim(
                r.firstName().strip(),
                r.lastName().strip(),
                r.email().strip(),
                blankToNull(r.phone()),
                r.policyNumber().strip(),
                r.lossDate(),
                r.lossType(),
                new Address(a.line1().strip(), blankToNull(a.line2()), a.city().strip(), a.province(), a.postalCode()),
                r.description().strip());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
