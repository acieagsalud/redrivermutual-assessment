package com.claimportal.notification.email;

/** Delivers an email. Throws if the provider did not accept it, so the caller can retry. */
public interface EmailSender {

    void send(EmailMessage message);
}
