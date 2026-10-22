package com.claimportal.notification.email;

import com.claimportal.notification.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Sends over SMTP. The same class talks to Mailpit locally and to Resend in the {@code resend}
 * profile; only configuration differs.
 *
 * <p>Two headers carry the event id so a retried send is recognised as the same email:
 * <ul>
 *   <li>{@code Resend-Idempotency-Key}: Resend returns the original result for a repeated key
 *       within 24 hours instead of sending again. Other SMTP servers ignore it.</li>
 *   <li>{@code Message-ID}: a stable id lets some mail clients collapse a duplicate if one ever
 *       gets through (for example a replay after the 24-hour window).</li>
 * </ul>
 */
@Component
public class SmtpEmailSender implements EmailSender {

    static final String IDEMPOTENCY_HEADER = "Resend-Idempotency-Key";

    private final JavaMailSender mailSender;
    private final MailProperties properties;

    public SmtpEmailSender(JavaMailSender mailSender, MailProperties properties) {
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Override
    public void send(EmailMessage email) {
        mailSender.send(toMimeMessage(email));
    }

    MimeMessage toMimeMessage(EmailMessage email) {
        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, true, StandardCharsets.UTF_8.name());
            helper.setFrom(properties.from());
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.textBody(), email.htmlBody());
            String key = email.idempotencyKey().toString();
            mime.setHeader(IDEMPOTENCY_HEADER, key);
            // JavaMailSenderImpl keeps an explicitly set Message-ID instead of generating a random one.
            mime.setHeader("Message-ID", "<" + key + "@" + properties.messageIdDomain() + ">");
            return mime;
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not build confirmation email", e);
        }
    }
}
