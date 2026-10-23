package com.claimportal.claims.application;

public class ClaimNumberUnavailableException extends RuntimeException {

    public ClaimNumberUnavailableException(int attempts) {
        super("Could not generate an unused claim number after " + attempts + " attempts");
    }
}
