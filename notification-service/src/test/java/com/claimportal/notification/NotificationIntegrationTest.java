package com.claimportal.notification;

import com.claimportal.notification.support.MailpitClient;
import com.claimportal.notification.support.MailpitClient.CapturedEmail;
import com.claimportal.notification.support.TestEvents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * The consumer end to end: a real RabbitMQ message in, a real email out (captured by Mailpit),
 * with PostgreSQL backing the duplicate check.
 */
@SpringBootTest(properties = {
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
        "spring.rabbitmq.listener.simple.retry.max-interval=200ms"
})
@Testcontainers
class NotificationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Container
    static GenericContainer<?> mailpit = new GenericContainer<>("axllent/mailpit:v1.27").withExposedPorts(1025, 8025);

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", mailpit::getHost);
        registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    private MailpitClient mailbox;

    @BeforeEach
    void setUp() {
        mailbox = new MailpitClient("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025));
        mailbox.deleteAll();
        jdbc.update("DELETE FROM processed_event");
        amqpAdmin.purgeQueue("notification.claim-submitted.dlq", false);
    }

    @Test
    void sendsOneConfirmationEmailContainingTheClaimNumber() {
        UUID eventId = UUID.randomUUID();
        publish(objectMapper.writeValueAsString(TestEvents.event(eventId, "Dana")));

        await().atMost(Duration.ofSeconds(10)).until(() -> mailbox.messages().size() == 1);

        CapturedEmail email = mailbox.messages().getFirst();
        assertThat(email.subject()).isEqualTo("We've received your claim CLM-7K4Q-9XHM");
        assertThat(email.to()).isEqualTo("dana@example.com");
        assertThat(mailbox.header(email.id(), "Resend-Idempotency-Key")).isEqualTo(eventId.toString());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, eventId))
                .isEqualTo(1);
    }

    @Test
    void theSameEventDeliveredTwiceSendsOnlyOneEmail() {
        String body = objectMapper.writeValueAsString(TestEvents.event(UUID.randomUUID(), "Dana"));
        publish(body);
        publish(body);

        await().atMost(Duration.ofSeconds(10)).until(() -> mailbox.messages().size() == 1);
        // Give the duplicate time to be consumed, then confirm it did not produce a second email.
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).until(() -> mailbox.messages().size() == 1);
    }

    @Test
    void anUnreadableMessageGoesToTheDeadLetterQueueAndSendsNothing() {
        publish("{not json");

        Message deadLettered = rabbitTemplate.receive("notification.claim-submitted.dlq", 10_000);

        assertThat(deadLettered).isNotNull();
        assertThat(new String(deadLettered.getBody())).isEqualTo("{not json");
        List<CapturedEmail> emails = mailbox.messages();
        assertThat(emails).isEmpty();
    }

    private void publish(String json) {
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        props.setHeader("X-Correlation-Id", "it-" + UUID.randomUUID());
        rabbitTemplate.send("claims.events", "claim.submitted", new Message(json.getBytes(), props));
    }
}
