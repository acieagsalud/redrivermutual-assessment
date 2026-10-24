package com.claimportal.claims.web;

/** The Idempotency-Key header is present but not 1 to 64 letters, digits or hyphens. */
public class InvalidIdempotencyKeyException extends RuntimeException {

    public InvalidIdempotencyKeyException() {
        super("Idempotency-Key must be 1 to 64 letters, digits or hyphens");
    }
}
