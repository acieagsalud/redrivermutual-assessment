package com.claimportal.notification.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.mail.MailSendException;

import static org.assertj.core.api.Assertions.assertThat;

class RetryConfigTest {

    @Test
    void retriesTransientFailures() {
        assertThat(RetryConfig.isRetryable(new ListenerExecutionFailedException("failed", new MailSendException("down")))).isTrue();
    }

    @Test
    void doesNotRetryMessagesTheListenerRejected() {
        assertThat(RetryConfig.isRetryable(
                new ListenerExecutionFailedException("failed", new AmqpRejectAndDontRequeueException("bad json")))).isFalse();
    }
}
