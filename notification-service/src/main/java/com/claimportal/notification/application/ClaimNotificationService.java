package com.claimportal.notification.application;

import com.claimportal.notification.email.EmailComposer;
import com.claimportal.notification.email.EmailSender;
import com.claimportal.notification.messaging.ClaimSubmittedEvent;
import com.claimportal.notification.persistence.ProcessedEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Sends the confirmation email for a ClaimSubmitted event, once per event.
 *
 * <p>Order inside one transaction: mark the event processed, then send. If the send throws, the
 * transaction rolls back, the mark disappears and the broker redelivers. If the process dies after
 * the provider accepted the email but before commit, the redelivery sends again with the same
 * idempotency key, and Resend drops it.
 */
@Service
public class ClaimNotificationService {

    private static final Logger log = LoggerFactory.getLogger(ClaimNotificationService.class);

    private final ProcessedEventRepository processedEvents;
    private final EmailComposer composer;
    private final EmailSender sender;
    private final Clock clock;
    private final Counter emailsSent;
    private final Counter duplicatesSkipped;

    public ClaimNotificationService(ProcessedEventRepository processedEvents, EmailComposer composer,
                                    EmailSender sender, Clock clock, MeterRegistry meterRegistry) {
        this.processedEvents = processedEvents;
        this.composer = composer;
        this.sender = sender;
        this.clock = clock;
        this.emailsSent = emailCounter(meterRegistry, "sent");
        this.duplicatesSkipped = emailCounter(meterRegistry, "duplicate_skipped");
    }

    private static Counter emailCounter(MeterRegistry registry, String outcome) {
        return Counter.builder("notification.emails")
                .description("ClaimSubmitted events handled, by outcome")
                .tag("outcome", outcome)
                .register(registry);
    }

    @Transactional
    public void handle(ClaimSubmittedEvent event) {
        if (!processedEvents.markProcessed(event.eventId(), event.claimNumber(), clock.instant())) {
            log.info("Duplicate event skipped eventId={} claimNumber={}", event.eventId(), event.claimNumber());
            duplicatesSkipped.increment();
            return;
        }
        sender.send(composer.claimReceived(event));
        emailsSent.increment();
        // The recipient address is personal information: log the claim number only.
        log.info("Confirmation email sent eventId={} claimNumber={}", event.eventId(), event.claimNumber());
    }
}
