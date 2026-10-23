package com.claimportal.claims;

import com.claimportal.claims.outbox.OutboxRelay;
import com.claimportal.claims.outbox.OutboxWriter;
import com.claimportal.claims.support.TestClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.IllegalTransactionStateException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the real application against real PostgreSQL and RabbitMQ (Testcontainers) to prove the
 * outbox guarantees that unit tests with mocks cannot: the transaction boundary and the broker round trip.
 */
@SpringBootTest(properties = {
        // The test drives the relay itself so it can assert before and after publishing.
        "claims.outbox.poll-interval-ms=3600000"
})
@AutoConfigureMockMvc
@Testcontainers
class ClaimSubmissionIntegrationTest {

    private static final String TEST_QUEUE = "test.claim-submitted";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private TopicExchange claimsEventsExchange;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboxWriter outboxWriter;

    private final LocalDate yesterday = LocalDate.now(ZoneId.of("America/Winnipeg")).minusDays(1);

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM outbox_event");
        jdbc.update("DELETE FROM claim");
        Queue queue = new Queue(TEST_QUEUE, false);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(claimsEventsExchange).with("claim.submitted"));
        amqpAdmin.purgeQueue(TEST_QUEUE, false);
    }

    @Test
    void submissionSavesClaimAndOutboxRowThenRelayDeliversTheEvent() throws Exception {
        mvc.perform(post("/api/v1/claims").contentType(MediaType.APPLICATION_JSON)
                        .content(TestClaims.validRequestJson(yesterday)))
                .andExpect(status().isCreated());

        String claimNumber = jdbc.queryForObject("SELECT claim_number FROM claim", String.class);
        Map<String, Object> outbox = jdbc.queryForMap("SELECT id, payload::text AS payload, published_at FROM outbox_event");
        assertThat(outbox.get("published_at")).as("not sent until the relay runs").isNull();

        JsonNode payload = objectMapper.readTree((String) outbox.get("payload"));
        assertThat(fieldNames(payload)).as("payload has exactly the contract's fields").isEqualTo(contractFieldNames());
        assertThat(payload.get("claimNumber").asString()).isEqualTo(claimNumber);
        assertThat(payload.get("occurredAt").asString()).as("ISO-8601, not epoch numbers").endsWith("Z");

        assertThat(relay.publishPending()).isEqualTo(1);

        Message message = rabbitTemplate.receive(TEST_QUEUE, 5_000);
        assertThat(message).isNotNull();
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo(outbox.get("id").toString());
        JsonNode delivered = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        assertThat(delivered.get("claimNumber").asString()).isEqualTo(claimNumber);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event WHERE published_at IS NOT NULL", Integer.class))
                .isEqualTo(1);
        assertThat(relay.publishPending()).as("nothing left to send").isZero();
    }

    @Test
    void claimIsRolledBackWhenTheOutboxInsertFails() throws Exception {
        // Make the database itself reject the outbox row. The claim row is inserted first in the
        // same transaction, so this proves a real rollback rather than a mocked one.
        jdbc.execute("ALTER TABLE outbox_event ADD CONSTRAINT test_reject_all CHECK (event_type <> 'ClaimSubmitted')");
        try {
            mvc.perform(post("/api/v1/claims").contentType(MediaType.APPLICATION_JSON)
                            .content(TestClaims.validRequestJson(yesterday)))
                    .andExpect(status().isInternalServerError());
        } finally {
            jdbc.execute("ALTER TABLE outbox_event DROP CONSTRAINT test_reject_all");
        }

        // The core outbox guarantee: no claim exists without its event.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM claim", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM outbox_event", Integer.class)).isZero();
    }

    @Test
    void outboxWriterRefusesToRunOutsideATransaction() {
        assertThatThrownBy(() -> outboxWriter.append(UUID.randomUUID(), null))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    private static Set<String> fieldNames(JsonNode node) {
        Set<String> names = new HashSet<>();
        node.propertyNames().forEach(names::add);
        return names;
    }

    private Set<String> contractFieldNames() throws Exception {
        return fieldNames(objectMapper.readTree(Files.readString(Path.of("..", "contracts", "claim-submitted.v1.json"))));
    }
}
