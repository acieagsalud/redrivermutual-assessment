package com.claimportal.claims.observability;

import org.slf4j.MDC;

import java.util.regex.Pattern;

/** Correlation id carried from the HTTP request, through the outbox, into the broker message headers. */
public final class CorrelationId {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    /** Client-supplied ids end up in logs, so only a safe, bounded format is accepted. */
    private static final Pattern SAFE = Pattern.compile("^[A-Za-z0-9-]{1,64}$");

    private CorrelationId() {
    }

    public static boolean isSafe(String value) {
        return value != null && SAFE.matcher(value).matches();
    }

    public static String current() {
        return MDC.get(MDC_KEY);
    }
}
