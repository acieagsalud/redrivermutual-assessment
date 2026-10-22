package com.claimportal.claims.application;

import com.claimportal.claims.domain.Address;
import com.claimportal.claims.domain.LossType;

import java.time.LocalDate;

/** A validated claim submission, independent of how it arrived (HTTP today). */
public record NewClaim(
        String firstName,
        String lastName,
        String email,
        String phone,
        String policyNumber,
        LocalDate lossDate,
        LossType lossType,
        Address address,
        String description) {
}
