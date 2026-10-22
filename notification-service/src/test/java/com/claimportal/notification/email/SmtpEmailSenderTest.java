package com.claimportal.notification.email;

import com.claimportal.notification.config.MailProperties;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmtpEmailSenderTest {

    @Test
    void stampsTheEventIdAsIdempotencyKeyAndMessageId() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        SmtpEmailSender sender = new SmtpEmailSender(mailSender,
                new MailProperties("Claims Team <claims@claimportal.local>", "claimportal.local"));
        UUID key = UUID.fromString("3f6c2b1e-8d4a-4c1f-9b2e-5a7d0e9c1f42");

        MimeMessage mime = sender.toMimeMessage(
                new EmailMessage(key, "dana@example.com", "We've received your claim CLM-7K4Q-9XHM", "text", "<p>html</p>"));

        assertThat(mime.getHeader("Resend-Idempotency-Key", null)).isEqualTo(key.toString());
        assertThat(mime.getHeader("Message-ID", null)).isEqualTo("<" + key + "@claimportal.local>");
        assertThat(mime.getSubject()).isEqualTo("We've received your claim CLM-7K4Q-9XHM");
        assertThat(((InternetAddress) mime.getAllRecipients()[0]).getAddress()).isEqualTo("dana@example.com");
        assertThat(((InternetAddress) mime.getFrom()[0]).getAddress()).isEqualTo("claims@claimportal.local");
    }
}
