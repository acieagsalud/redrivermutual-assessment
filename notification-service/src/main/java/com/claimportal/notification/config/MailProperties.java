package com.claimportal.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param from            sender address; with Resend and no verified domain this must be onboarding@resend.dev
 * @param messageIdDomain right-hand side of the Message-ID header, e.g. {@code <eventId@claims.local>}
 */
@ConfigurationProperties("notification.mail")
public record MailProperties(String from, String messageIdDomain) {
}
