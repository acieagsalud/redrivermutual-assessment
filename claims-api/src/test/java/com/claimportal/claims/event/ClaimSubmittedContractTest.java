package com.claimportal.claims.event;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

/** The producer side of contracts/claim-submitted.v1.json: what we write must match it exactly. */
class ClaimSubmittedContractTest {

    private static final Path FIXTURE = Path.of("..", "contracts", "claim-submitted.v1.json");

    @Test
    void serializedEventMatchesTheSharedContract() throws Exception {
        ClaimSubmittedEvent event = new ClaimSubmittedEvent(
                UUID.fromString("3f6c2b1e-8d4a-4c1f-9b2e-5a7d0e9c1f42"),
                ClaimSubmittedEvent.TYPE,
                ClaimSubmittedEvent.CURRENT_VERSION,
                Instant.parse("2026-10-22T18:30:00Z"),
                "CLM-7K4Q-9XHM",
                "Dana",
                "dana@example.com");

        String json = JsonMapper.builder().build().writeValueAsString(event);

        JSONAssert.assertEquals(Files.readString(FIXTURE), json, JSONCompareMode.STRICT);
    }
}
