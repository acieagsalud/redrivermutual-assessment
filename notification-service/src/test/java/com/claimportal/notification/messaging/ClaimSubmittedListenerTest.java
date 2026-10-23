package com.claimportal.notification.messaging;

import com.claimportal.notification.application.ClaimNotificationService;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ClaimSubmittedListenerTest {

    private final ClaimNotificationService service = mock(ClaimNotificationService.class);
    private final ClaimSubmittedListener listener = new ClaimSubmittedListener(service, JsonMapper.builder().build());

    @Test
    void handsAValidEventToTheServiceWithTheCorrelationIdInTheLogContext() throws Exception {
        AtomicReference<String> seen = new AtomicReference<>();
        doAnswer(inv -> {
            seen.set(MDC.get("correlationId"));
            return null;
        }).when(service).handle(any());

        listener.onMessage(message(Files.readString(Path.of("..", "contracts", "claim-submitted.v1.json")), "corr-42"));

        verify(service).handle(any());
        assertThat(seen.get()).isEqualTo("corr-42");
        assertThat(MDC.get("correlationId")).as("cleared afterwards").isNull();
    }

    @Test
    void rejectsMalformedJsonWithoutRetrying() {
        assertThatThrownBy(() -> listener.onMessage(message("{not json", null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
        verify(service, never()).handle(any());
    }

    @Test
    void rejectsAnEventMissingRequiredFields() {
        assertThatThrownBy(() -> listener.onMessage(message("{\"eventType\":\"ClaimSubmitted\"}", null)))
                .isInstanceOf(AmqpRejectAndDontRequeueException.class);
    }

    private static Message message(String body, String correlationId) {
        MessageProperties props = new MessageProperties();
        if (correlationId != null) {
            props.setHeader("X-Correlation-Id", correlationId);
        }
        return new Message(body.getBytes(StandardCharsets.UTF_8), props);
    }
}
