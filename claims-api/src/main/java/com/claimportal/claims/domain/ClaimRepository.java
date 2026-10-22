package com.claimportal.claims.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    boolean existsByClaimNumber(String claimNumber);
}
