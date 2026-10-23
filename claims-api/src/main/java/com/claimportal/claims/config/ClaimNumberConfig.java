package com.claimportal.claims.config;

import com.claimportal.claims.domain.ClaimNumberGenerator;
import com.claimportal.claims.domain.RandomClaimNumberGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClaimNumberConfig {

    @Bean
    ClaimNumberGenerator claimNumberGenerator() {
        return new RandomClaimNumberGenerator();
    }
}
