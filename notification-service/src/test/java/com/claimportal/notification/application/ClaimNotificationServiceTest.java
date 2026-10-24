package com.claimportal.notification.application;

import com.claimportal.notification.email.EmailComposer;
import com.claimportal.notification.email.EmailMessage;
import com.claimportal.notification.email.EmailSender;
import com.claimportal.notification.messaging.ClaimSubmittedEvent;
import com.claimportal.notification.persistence.ProcessedEventRepository;
import com.claimportal.notification.support.TestEvents;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimNotificationServiceTest {

    private final ProcessedEventRepository processedEvents = mock(ProcessedEventRepository.class);
    private final EmailComposer composer = mock(EmailComposer.class);
    private final EmailSender sender = mock(EmailSender.class);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final ClaimNotificationService service = new ClaimNotificationService(processedEvents, composer, sender,
            Clock.fixed(Instant.parse("2026-10-22T18:30:05Z"), ZoneOffset.UTC), meters);

    private final ClaimSubmittedEvent event = TestEvents.event();
    private final EmailMessage email = new EmailMessage(event.eventId(), "dana@example.com", "subject", "text", "<p>html</p>");

    @Test
    void sendsTheEmailForANewEvent() {
        when(processedEvents.markProcessed(eq(event.eventId()), anyString(), any())).thenReturn(true);
        when(composer.claimReceived(event)).thenReturn(email);

        service.handle(event);

        verify(sender).send(email);
        assertThat(meters.counter("notification.emails", "outcome", "sent").count()).isEqualTo(1);
    }

    @Test
    void skipsAnEventThatWasAlreadyProcessed() {
        when(processedEvents.markProcessed(eq(event.eventId()), anyString(), any())).thenReturn(false);

        service.handle(event);

        verify(sender, never()).send(any());
        assertThat(meters.counter("notification.emails", "outcome", "duplicate_skipped").count()).isEqualTo(1);
        assertThat(meters.counter("notification.emails", "outcome", "sent").count()).isZero();
    }

    @Test
    void propagatesSendFailuresSoTheTransactionRollsBackAndTheBrokerRetries() {
        when(processedEvents.markProcessed(eq(event.eventId()), anyString(), any())).thenReturn(true);
        when(composer.claimReceived(event)).thenReturn(email);
        doThrow(new MailSendException("SMTP unavailable")).when(sender).send(email);

        assertThatThrownBy(() -> service.handle(event)).isInstanceOf(MailSendException.class);
        assertThat(meters.counter("notification.emails", "outcome", "sent").count()).isZero();
    }
}
