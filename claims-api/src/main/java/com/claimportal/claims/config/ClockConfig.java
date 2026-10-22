package com.claimportal.claims.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * One injectable clock so "today" is testable and means the same thing everywhere.
 * The zone matters for loss-date validation: late evening in Winnipeg is already tomorrow in UTC.
 */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(@Value("${claims.time-zone:America/Winnipeg}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
