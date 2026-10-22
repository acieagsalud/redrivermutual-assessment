package com.claimportal.notification.email;

import com.claimportal.notification.messaging.ClaimSubmittedEvent;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders the claim confirmation.
 *
 * <p>The output depends only on the event, never on the current time, so a retry produces the
 * same email. Resend rejects a reused idempotency key whose content differs. Thymeleaf's
 * {@code th:text} HTML-escapes the customer's name, so input like {@code <script>} renders as text.
 */
@Component
public class EmailComposer {

    static final String TEMPLATE = "email/claim-received";

    private final ITemplateEngine templateEngine;

    public EmailComposer(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public EmailMessage claimReceived(ClaimSubmittedEvent event) {
        String subject = "We've received your claim " + event.claimNumber();

        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("firstName", event.customerFirstName());
        context.setVariable("claimNumber", event.claimNumber());
        String html = templateEngine.process(TEMPLATE, context);

        String text = """
                Hi %s,

                Thank you for reporting your loss. We've received your home insurance claim.

                Your claim number is %s.

                Please keep this number handy. An adjuster will contact you to discuss next steps.
                If you need to reach us before then, quote your claim number so we can find your file quickly.

                Claims Team

                This is an automated message. Please do not reply to this email.
                """.formatted(event.customerFirstName(), event.claimNumber());

        return new EmailMessage(event.eventId(), event.customerEmail(), subject, text, html);
    }
}
