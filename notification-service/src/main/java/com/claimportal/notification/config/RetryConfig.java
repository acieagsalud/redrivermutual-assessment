package com.claimportal.notification.config;

import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.boot.amqp.autoconfigure.RabbitListenerRetrySettingsCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Retries are for transient failures (SMTP down, database blip). A message the listener has
 * already declared unprocessable skips the retries and goes straight to the dead-letter queue.
 */
@Configuration
public class RetryConfig {

    @Bean
    RabbitListenerRetrySettingsCustomizer skipRetryForRejectedMessages() {
        return settings -> settings.setExceptionPredicate(RetryConfig::isRetryable);
    }

    static boolean isRetryable(Throwable failure) {
        for (Throwable t = failure; t != null; t = t.getCause()) {
            if (t instanceof AmqpRejectAndDontRequeueException) {
                return false;
            }
        }
        return true;
    }
}
