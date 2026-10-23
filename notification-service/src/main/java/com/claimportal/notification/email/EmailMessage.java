package com.claimportal.notification.email;

import java.util.UUID;

/**
 * A rendered email. {@code idempotencyKey} must be identical on every retry of the same event so the
 * provider can recognise a repeat.
 */
public record EmailMessage(UUID idempotencyKey, String to, String subject, String textBody, String htmlBody) {
}
