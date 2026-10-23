package com.claimportal.claims.web.dto;

import com.claimportal.claims.domain.LossType;
import com.claimportal.claims.web.validation.ValidLossDate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of POST /api/v1/claims. These rules are the source of truth; the web form mirrors them
 * for faster feedback but the server never trusts the client.
 */
public record SubmitClaimRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @Pattern(regexp = "^$|^\\(?\\d{3}\\)?[ .-]?\\d{3}[ .-]?\\d{4}$", message = "must be a 10-digit phone number")
        String phone,
        // Assumed format: no policy system is available to check against (see README assumptions).
        @NotBlank @Pattern(regexp = "^HO-\\d{8}$", message = "must look like HO-12345678")
        String policyNumber,
        @NotNull @ValidLossDate LocalDate lossDate,
        @NotNull LossType lossType,
        @NotNull @Valid AddressRequest address,
        @NotBlank @Size(min = 20, max = 2000) String description) {
}
