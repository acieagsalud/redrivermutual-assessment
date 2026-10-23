package com.claimportal.claims.support;

import com.claimportal.claims.application.NewClaim;
import com.claimportal.claims.domain.Address;
import com.claimportal.claims.domain.LossType;
import com.claimportal.claims.domain.Province;

import java.time.LocalDate;

/** Valid sample data shared by tests. */
public final class TestClaims {

    private TestClaims() {
    }

    public static NewClaim newClaim() {
        return new NewClaim("Dana", "Smith", "dana@example.com", "204-555-0142", "HO-12345678",
                LocalDate.now().minusDays(3), LossType.SEWER_BACKUP,
                new Address("123 Main St", null, "Winnipeg", Province.MB, "r3c1a1"),
                "Basement flooded after heavy rain; carpet and drywall damaged.");
    }

    /** A valid request body; tests override single fields with String.replace for negative cases. */
    public static String validRequestJson(LocalDate lossDate) {
        return """
                {
                  "firstName": "Dana",
                  "lastName": "Smith",
                  "email": "dana@example.com",
                  "phone": "204-555-0142",
                  "policyNumber": "HO-12345678",
                  "lossDate": "%s",
                  "lossType": "SEWER_BACKUP",
                  "address": {
                    "line1": "123 Main St",
                    "line2": "",
                    "city": "Winnipeg",
                    "province": "MB",
                    "postalCode": "R3C 1A1"
                  },
                  "description": "Basement flooded after heavy rain; carpet and drywall damaged."
                }
                """.formatted(lossDate);
    }
}
