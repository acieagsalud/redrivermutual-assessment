package com.claimportal.claims.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A submitted home insurance claim.
 *
 * <p>The JPA entity doubles as the domain model. A separate persistence model would add mapping
 * code with no behaviour to protect at this size; that trade-off would change as the claim
 * lifecycle grows.
 */
@Entity
@Table(name = "claim")
public class Claim implements Persistable<UUID> {

    // The id is assigned in code, so without this Spring Data would call merge() and SELECT before every INSERT.
    @Transient
    private boolean isNew = true;

    @Id
    private UUID id;

    @Column(name = "claim_number", nullable = false, unique = true, length = 13)
    private String claimNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClaimStatus status;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    private String phone;

    @Column(name = "policy_number", nullable = false)
    private String policyNumber;

    @Column(name = "loss_date", nullable = false)
    private LocalDate lossDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "loss_type", nullable = false)
    private LossType lossType;

    @Embedded
    private Address address;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    protected Claim() {
        // for JPA
    }

    private Claim(Builder b) {
        this.id = b.id;
        this.claimNumber = b.claimNumber;
        this.status = ClaimStatus.SUBMITTED;
        this.firstName = b.firstName;
        this.lastName = b.lastName;
        this.email = b.email;
        this.phone = b.phone;
        this.policyNumber = b.policyNumber;
        this.lossDate = b.lossDate;
        this.lossType = b.lossType;
        this.address = b.address;
        this.description = b.description;
        this.createdAt = b.createdAt;
        this.idempotencyKey = b.idempotencyKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() { return id; }
    public String getClaimNumber() { return claimNumber; }
    public ClaimStatus getStatus() { return status; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPolicyNumber() { return policyNumber; }
    public LocalDate getLossDate() { return lossDate; }
    public LossType getLossType() { return lossType; }
    public Address getAddress() { return address; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
    public String getIdempotencyKey() { return idempotencyKey; }

    public static final class Builder {
        private UUID id;
        private String claimNumber;
        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String policyNumber;
        private LocalDate lossDate;
        private LossType lossType;
        private Address address;
        private String description;
        private Instant createdAt;
        private String idempotencyKey;

        public Builder id(UUID id) { this.id = id; return this; }
        public Builder claimNumber(String claimNumber) { this.claimNumber = claimNumber; return this; }
        public Builder firstName(String firstName) { this.firstName = firstName; return this; }
        public Builder lastName(String lastName) { this.lastName = lastName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder policyNumber(String policyNumber) { this.policyNumber = policyNumber; return this; }
        public Builder lossDate(LocalDate lossDate) { this.lossDate = lossDate; return this; }
        public Builder lossType(LossType lossType) { this.lossType = lossType; return this; }
        public Builder address(Address address) { this.address = address; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder idempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; return this; }

        public Claim build() {
            return new Claim(this);
        }
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        isNew = false;
    }
}
