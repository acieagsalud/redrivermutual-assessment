package com.claimportal.notification;

import com.claimportal.notification.support.TestEvents;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** With the mail server unreachable, the event is retried, then dead-lettered, and not marked processed. */
@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=1", // nothing listens here
        "spring.rabbitmq.listener.simple.retry.initial-interval=100ms",
        "spring.rabbitmq.listener.simple.retry.max-interval=200ms"
})
@Testcontainers
class SmtpOutageIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void failedSendIsRetriedThenDeadLetteredWithoutBeingMarkedProcessed() {
        UUID eventId = UUID.randomUUID();
        MessageProperties props = new MessageProperties();
        props.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        rabbitTemplate.send("claims.events", "claim.submitted",
                new Message(objectMapper.writeValueAsBytes(TestEvents.event(eventId, "Dana")), props));

        Message deadLettered = rabbitTemplate.receive("notification.claim-submitted.dlq", 15_000);

        assertThat(deadLettered).as("dead-lettered after retries").isNotNull();
        // The rollback removed the processed mark, so replaying from the DLQ will send the email.
        assertThat(jdbc.queryForObject("SELECT count(*) FROM processed_event WHERE event_id = ?", Integer.class, eventId))
                .isZero();
    }
}
