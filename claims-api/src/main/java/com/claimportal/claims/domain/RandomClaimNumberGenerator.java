package com.claimportal.claims.domain;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

/**
 * Generates numbers like {@code CLM-7K4Q-9XHM}.
 *
 * <p>Random rather than sequential so numbers cannot be guessed or used to estimate volume.
 * The alphabet drops 0/O, 1/I/L and U so a number can be read over the phone without confusion.
 * 30^8 (about 6.5 x 10^11) combinations make collisions rare; the caller retries if one happens.
 */
public class RandomClaimNumberGenerator implements ClaimNumberGenerator {

    static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTVWXYZ";
    static final String PREFIX = "CLM-";

    private final RandomGenerator random;

    public RandomClaimNumberGenerator() {
        this(new SecureRandom());
    }

    RandomClaimNumberGenerator(RandomGenerator random) {
        this.random = random;
    }

    @Override
    public String next() {
        StringBuilder sb = new StringBuilder(PREFIX);
        appendBlock(sb);
        sb.append('-');
        appendBlock(sb);
        return sb.toString();
    }

    private void appendBlock(StringBuilder sb) {
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
    }
}
