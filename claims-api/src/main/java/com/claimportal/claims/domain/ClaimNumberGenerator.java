package com.claimportal.claims.domain;

/** Produces customer-facing claim numbers. Uniqueness is enforced by the caller and the database. */
public interface ClaimNumberGenerator {

    String next();
}
