package com.claimportal.claims.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    @ParameterizedTest
    @ValueSource(strings = {"R3C 1A1", "r3c1a1", "R3C-1A1", " r3c 1a1 "})
    void normalizesPostalCodeToCanonicalForm(String typed) {
        Address address = new Address("123 Main St", null, "Winnipeg", Province.MB, typed);
        assertThat(address.getPostalCode()).isEqualTo("R3C 1A1");
    }
}
