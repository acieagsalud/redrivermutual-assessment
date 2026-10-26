package com.claimportal.claims.application;

import com.claimportal.claims.domain.Claim;
import com.claimportal.claims.domain.ClaimNumberGenerator;
import com.claimportal.claims.domain.ClaimRepository;
import com.claimportal.claims.event.ClaimSubmittedEvent;
import com.claimportal.claims.outbox.OutboxWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

/**
 * Accepts a claim: assigns a claim number, saves it and records the ClaimSubmitted event,
 * all in one database transaction. Either both rows exist afterwards or neither does.
 */
@Service
public class ClaimSubmissionService {

    static final int MAX_CLAIM_NUMBER_ATTEMPTS = 3;

    private static final Logger log = LoggerFactory.getLogger(ClaimSubmissionService.class);

    private final ClaimRepository claimRepository;
    private final ClaimNumberGenerator claimNumberGenerator;
    private final OutboxWriter outboxWriter;
    private final Clock clock;
    private final TransactionOperations transactions;

    public ClaimSubmissionService(ClaimRepository claimRepository, ClaimNumberGenerator claimNumberGenerator,
                                  OutboxWriter outboxWriter, Clock clock, TransactionOperations transactions) {
        this.claimRepository = claimRepository;
        this.claimNumberGenerator = claimNumberGenerator;
        this.outboxWriter = outboxWriter;
        this.clock = clock;
        this.transactions = transactions;
    }

    /**
     * Saves a new claim, or returns the one already created with the same idempotency key. A null key
     * always creates a claim.
     *
     * <p>The transaction is managed here rather than with {@code @Transactional} because a duplicate key
     * only shows up when the insert commits: two concurrent requests with the same new key can both pass
     * the lookup, the unique index rejects the second, and that request then returns the first one's claim
     * from a fresh read.
     */
    public SubmittedClaim submit(NewClaim request, String idempotencyKey) {
        try {
            return transactions.execute(tx -> submitInTransaction(request, idempotencyKey));
        } catch (DataIntegrityViolationException e) {
            if (idempotencyKey == null) {
                throw e;
            }
            return findExisting(idempotencyKey).orElseThrow(() -> e);
        }
    }

    private SubmittedClaim submitInTransaction(NewClaim request, String idempotencyKey) {
        if (idempotencyKey != null) {
            Optional<SubmittedClaim> existing = findExisting(idempotencyKey);
            if (existing.isPresent()) {
                log.info("Repeated submission returned existing claimNumber={}", existing.get().claimNumber());
                return existing.get();
            }
        }
        // PostgreSQL stores microseconds; truncating keeps a replayed response identical to the first one.
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Claim claim = Claim.builder()
                .id(UUID.randomUUID())
                .claimNumber(unusedClaimNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .phone(request.phone())
                .policyNumber(request.policyNumber())
                .lossDate(request.lossDate())
                .lossType(request.lossType())
                .address(request.address())
                .description(request.description())
                .createdAt(now)
                .idempotencyKey(idempotencyKey)
                .build();

        claimRepository.save(claim);
        outboxWriter.append(claim.getId(), ClaimSubmittedEvent.from(claim, UUID.randomUUID(), now));

        // Claim number only: name, email and address are personal information and stay out of logs.
        log.info("Claim submitted claimNumber={} lossType={}", claim.getClaimNumber(), claim.getLossType());
        return new SubmittedClaim(claim.getClaimNumber(), claim.getStatus(), now);
    }

    private Optional<SubmittedClaim> findExisting(String idempotencyKey) {
        return claimRepository.findByIdempotencyKey(idempotencyKey)
                .map(c -> new SubmittedClaim(c.getClaimNumber(), c.getStatus(), c.getCreatedAt(), true));
    }

    /**
     * Checks for a clash before inserting. The unique constraint remains the final guard: two
     * requests drawing the same number at the same instant would fail one of them with a 500,
     * which at roughly 1 in 6.5 x 10^11 per pair is an accepted risk.
     */
    private String unusedClaimNumber() {
        for (int attempt = 1; attempt <= MAX_CLAIM_NUMBER_ATTEMPTS; attempt++) {
            String candidate = claimNumberGenerator.next();
            if (!claimRepository.existsByClaimNumber(candidate)) {
                return candidate;
            }
            log.warn("Claim number collision on attempt {}", attempt);
        }
        throw new ClaimNumberUnavailableException(MAX_CLAIM_NUMBER_ATTEMPTS);
    }
}
