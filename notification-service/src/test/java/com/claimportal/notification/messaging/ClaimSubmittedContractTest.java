package com.claimportal.notification.messaging;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The consumer side of contracts/claim-submitted.v1.json: we must be able to read what claims-api writes. */
class ClaimSubmittedContractTest {

    private static final Path FIXTURE = Path.of("..", "contracts", "claim-submitted.v1.json");

    @Test
    void readsTheSharedContract() throws Exception {
        ClaimSubmittedEvent event = JsonMapper.builder().build().readValue(Files.readString(FIXTURE), ClaimSubmittedEvent.class);

        assertThat(event.eventId()).isEqualTo(UUID.fromString("3f6c2b1e-8d4a-4c1f-9b2e-5a7d0e9c1f42"));
        assertThat(event.eventType()).isEqualTo("ClaimSubmitted");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-10-22T18:30:00Z"));
        assertThat(event.claimNumber()).isEqualTo("CLM-7K4Q-9XHM");
        assertThat(event.customerFirstName()).isEqualTo("Dana");
        assertThat(event.customerEmail()).isEqualTo("dana@example.com");
    }

    @Test
    void ignoresFieldsAddedByANewerProducer() throws Exception {
        String withExtraField = Files.readString(FIXTURE).replace("\"version\": 1,", "\"version\": 1, \"lossType\": \"FIRE\",");

        ClaimSubmittedEvent event = JsonMapper.builder().build().readValue(withExtraField, ClaimSubmittedEvent.class);

        assertThat(event.claimNumber()).isEqualTo("CLM-7K4Q-9XHM");
    }
}
