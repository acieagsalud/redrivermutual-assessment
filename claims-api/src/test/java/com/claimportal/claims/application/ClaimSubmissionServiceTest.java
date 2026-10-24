package com.claimportal.claims.application;

import com.claimportal.claims.domain.Claim;
import com.claimportal.claims.domain.ClaimNumberGenerator;
import com.claimportal.claims.domain.ClaimRepository;
import com.claimportal.claims.domain.ClaimStatus;
import com.claimportal.claims.event.ClaimSubmittedEvent;
import com.claimportal.claims.outbox.OutboxWriter;
import com.claimportal.claims.support.TestClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimSubmissionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-22T18:30:00Z");

    private final ClaimRepository claimRepository = mock(ClaimRepository.class);
    private final ClaimNumberGenerator generator = mock(ClaimNumberGenerator.class);
    private final OutboxWriter outboxWriter = mock(OutboxWriter.class);
    private ClaimSubmissionService service;

    @BeforeEach
    void setUp() {
        service = new ClaimSubmissionService(claimRepository, generator, outboxWriter, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void savesClaimAndRecordsMatchingEvent() {
        when(generator.next()).thenReturn("CLM-AAAA-BBBB");

        SubmittedClaim result = service.submit(TestClaims.newClaim(), null);

        assertThat(result.claimNumber()).isEqualTo("CLM-AAAA-BBBB");
        assertThat(result.status()).isEqualTo(ClaimStatus.SUBMITTED);
        assertThat(result.submittedAt()).isEqualTo(NOW);

        ArgumentCaptor<Claim> claim = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(claim.capture());
        assertThat(claim.getValue().getClaimNumber()).isEqualTo("CLM-AAAA-BBBB");
        assertThat(claim.getValue().getAddress().getPostalCode()).isEqualTo("R3C 1A1");

        ArgumentCaptor<ClaimSubmittedEvent> event = ArgumentCaptor.forClass(ClaimSubmittedEvent.class);
        verify(outboxWriter).append(eq(claim.getValue().getId()), event.capture());
        assertThat(event.getValue().claimNumber()).isEqualTo("CLM-AAAA-BBBB");
        assertThat(event.getValue().customerFirstName()).isEqualTo("Dana");
        assertThat(event.getValue().customerEmail()).isEqualTo("dana@example.com");
        assertThat(event.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void drawsAnotherNumberWhenTheFirstIsTaken() {
        when(generator.next()).thenReturn("CLM-TAKE-NNNN", "CLM-FREE-NNNN");
        when(claimRepository.existsByClaimNumber("CLM-TAKE-NNNN")).thenReturn(true);

        assertThat(service.submit(TestClaims.newClaim(), null).claimNumber()).isEqualTo("CLM-FREE-NNNN");
    }

    @Test
    void givesUpAfterThreeCollisionsWithoutSavingAnything() {
        when(generator.next()).thenReturn("CLM-TAKE-NNNN");
        when(claimRepository.existsByClaimNumber(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.submit(TestClaims.newClaim(), null))
                .isInstanceOf(ClaimNumberUnavailableException.class);
        verify(claimRepository, never()).save(any());
        verify(outboxWriter, never()).append(any(UUID.class), any());
    }

    @Test
    void storesTheIdempotencyKeyOnANewClaim() {
        when(generator.next()).thenReturn("CLM-AAAA-BBBB");

        SubmittedClaim result = service.submit(TestClaims.newClaim(), "key-1");

        assertThat(result.replayed()).isFalse();
        ArgumentCaptor<Claim> claim = ArgumentCaptor.forClass(Claim.class);
        verify(claimRepository).save(claim.capture());
        assertThat(claim.getValue().getIdempotencyKey()).isEqualTo("key-1");
    }

    @Test
    void aRepeatedKeyReturnsTheExistingClaimWithoutSavingOrQueueingAnotherEmail() {
        Claim first = savedClaimWithKey("key-1");
        when(claimRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(first));

        SubmittedClaim result = service.submit(TestClaims.newClaim(), "key-1");

        assertThat(result.claimNumber()).isEqualTo(first.getClaimNumber());
        assertThat(result.submittedAt()).isEqualTo(first.getCreatedAt());
        assertThat(result.replayed()).isTrue();
        verify(claimRepository, never()).save(any());
        verify(outboxWriter, never()).append(any(UUID.class), any());
    }

    private static Claim savedClaimWithKey(String key) {
        NewClaim c = TestClaims.newClaim();
        return Claim.builder()
                .id(UUID.randomUUID())
                .claimNumber("CLM-FIRS-TONE")
                .firstName(c.firstName())
                .lastName(c.lastName())
                .email(c.email())
                .policyNumber(c.policyNumber())
                .lossDate(c.lossDate())
                .lossType(c.lossType())
                .address(c.address())
                .description(c.description())
                .createdAt(NOW.minusSeconds(5))
                .idempotencyKey(key)
                .build();
    }
}
