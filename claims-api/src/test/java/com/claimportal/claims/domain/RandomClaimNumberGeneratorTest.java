package com.claimportal.claims.domain;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RandomClaimNumberGeneratorTest {

    private final RandomClaimNumberGenerator generator = new RandomClaimNumberGenerator();

    @Test
    void producesPrefixAndTwoBlocksOfFour() {
        assertThat(generator.next()).matches("^CLM-[2-9A-Z]{4}-[2-9A-Z]{4}$").hasSize(13);
    }

    @Test
    void neverUsesCharactersThatAreEasyToMisread() {
        for (int i = 0; i < 1_000; i++) {
            assertThat(generator.next().substring(4)).doesNotContain("0", "O", "1", "I", "L", "U");
        }
    }

    @Test
    void isDeterministicForAGivenRandomSource() {
        var a = new RandomClaimNumberGenerator(new Random(42));
        var b = new RandomClaimNumberGenerator(new Random(42));
        assertThat(a.next()).isEqualTo(b.next());
    }

    @Test
    void tenThousandNumbersAreAllDistinct() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            seen.add(generator.next());
        }
        assertThat(seen).hasSize(10_000);
    }
}
