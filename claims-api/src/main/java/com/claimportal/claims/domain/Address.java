package com.claimportal.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class Address {

    @Column(name = "address_line1", nullable = false)
    private String line1;

    @Column(name = "address_line2")
    private String line2;

    @Column(nullable = false)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Province province;

    @Column(name = "postal_code", nullable = false, length = 7)
    private String postalCode;

    protected Address() {
        // for JPA
    }

    public Address(String line1, String line2, String city, Province province, String postalCode) {
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.province = province;
        this.postalCode = normalizePostalCode(postalCode);
    }

    /** Stores postal codes in the canonical "A1A 1A1" form regardless of how they were typed. */
    static String normalizePostalCode(String raw) {
        String compact = raw.replaceAll("[\\s-]", "").toUpperCase();
        return compact.substring(0, 3) + " " + compact.substring(3);
    }

    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public Province getProvince() { return province; }
    public String getPostalCode() { return postalCode; }
}
