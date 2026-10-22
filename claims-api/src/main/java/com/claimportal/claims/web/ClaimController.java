package com.claimportal.claims.web;

import com.claimportal.claims.application.ClaimSubmissionService;
import com.claimportal.claims.application.NewClaim;
import com.claimportal.claims.application.SubmittedClaim;
import com.claimportal.claims.domain.Address;
import com.claimportal.claims.web.dto.AddressRequest;
import com.claimportal.claims.web.dto.SubmitClaimRequest;
import com.claimportal.claims.web.dto.SubmitClaimResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/claims")
public class ClaimController {

    private final ClaimSubmissionService submissionService;

    public ClaimController(ClaimSubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    /**
     * 201 means the claim is saved and its confirmation email is queued. The email itself is sent
     * asynchronously by notification-service, so the client tells the customer to check their inbox.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SubmitClaimResponse submit(@Valid @RequestBody SubmitClaimRequest request) {
        SubmittedClaim result = submissionService.submit(toNewClaim(request));
        return new SubmitClaimResponse(result.claimNumber(), result.status().name(), result.submittedAt());
    }

    private static NewClaim toNewClaim(SubmitClaimRequest r) {
        AddressRequest a = r.address();
        return new NewClaim(
                r.firstName().strip(),
                r.lastName().strip(),
                r.email().strip(),
                blankToNull(r.phone()),
                r.policyNumber().strip(),
                r.lossDate(),
                r.lossType(),
                new Address(a.line1().strip(), blankToNull(a.line2()), a.city().strip(), a.province(), a.postalCode()),
                r.description().strip());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
