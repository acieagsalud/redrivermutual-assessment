package com.claimportal.notification.email;

import com.claimportal.notification.support.TestEvents;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailComposerTest {

    private final EmailComposer composer = new EmailComposer(engine());

    @Test
    void includesTheClaimNumberInSubjectAndBothBodies() {
        EmailMessage email = composer.claimReceived(TestEvents.event());

        assertThat(email.subject()).isEqualTo("We've received your claim CLM-7K4Q-9XHM");
        assertThat(email.htmlBody()).contains("CLM-7K4Q-9XHM").contains("Hi <span>Dana</span>");
        assertThat(email.textBody()).contains("Your claim number is CLM-7K4Q-9XHM.").startsWith("Hi Dana,");
        assertThat(email.to()).isEqualTo("dana@example.com");
    }

    @Test
    void usesTheEventIdAsTheIdempotencyKey() {
        UUID eventId = UUID.randomUUID();

        assertThat(composer.claimReceived(TestEvents.event(eventId, "Dana")).idempotencyKey()).isEqualTo(eventId);
    }

    @Test
    void escapesHtmlInTheCustomersName() {
        EmailMessage email = composer.claimReceived(TestEvents.event(UUID.randomUUID(), "<script>alert(1)</script>"));

        assertThat(email.htmlBody()).doesNotContain("<script>").contains("&lt;script&gt;");
    }

    @Test
    void rendersIdenticallyEveryTimeSoRetriesMatchTheIdempotencyKey() {
        var event = TestEvents.event();

        assertThat(composer.claimReceived(event)).isEqualTo(composer.claimReceived(event));
    }

    private static SpringTemplateEngine engine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
