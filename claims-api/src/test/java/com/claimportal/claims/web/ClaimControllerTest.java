package com.claimportal.claims.web;

import com.claimportal.claims.application.ClaimSubmissionService;
import com.claimportal.claims.application.NewClaim;
import com.claimportal.claims.application.SubmittedClaim;
import com.claimportal.claims.config.ClockConfig;
import com.claimportal.claims.domain.ClaimStatus;
import com.claimportal.claims.support.TestClaims;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClaimController.class)
@Import({ClockConfig.class, ClaimControllerTest.Metrics.class})
class ClaimControllerTest {

    private static final String URL = "/api/v1/claims";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClaimSubmissionService service;

    @Autowired
    private MeterRegistry meters;

    @TestConfiguration
    static class Metrics {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    private final LocalDate yesterday = LocalDate.now(ZoneId.of("America/Winnipeg")).minusDays(1);

    @Test
    void validSubmissionReturns201WithClaimNumber() throws Exception {
        // The registry is shared by every test in this cached context, so measure the change.
        double submittedBefore = meters.counter("claims.submitted").count();
        when(service.submit(any(), any())).thenReturn(
                new SubmittedClaim("CLM-7K4Q-9XHM", ClaimStatus.SUBMITTED, Instant.parse("2026-10-22T18:30:00Z")));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestClaims.validRequestJson(yesterday)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimNumber").value("CLM-7K4Q-9XHM"))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.submittedAt").value("2026-10-22T18:30:00Z"));

        assertThat(meters.counter("claims.submitted").count()).isEqualTo(submittedBefore + 1);
    }

    @Test
    void blankOptionalFieldsAreStoredAsNullAndTextIsTrimmed() throws Exception {
        when(service.submit(any(), any())).thenReturn(new SubmittedClaim("CLM-7K4Q-9XHM", ClaimStatus.SUBMITTED, Instant.now()));
        String body = TestClaims.validRequestJson(yesterday)
                .replace("\"phone\": \"204-555-0142\"", "\"phone\": \"\"")
                .replace("\"firstName\": \"Dana\"", "\"firstName\": \"  Dana  \"");

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

        ArgumentCaptor<NewClaim> captor = ArgumentCaptor.forClass(NewClaim.class);
        verify(service).submit(captor.capture(), isNull());
        assertThat(captor.getValue().phone()).isNull();
        assertThat(captor.getValue().address().getLine2()).isNull();
        assertThat(captor.getValue().firstName()).isEqualTo("Dana");
    }

    @Test
    void invalidFieldsReturn400WithAnErrorPerField() throws Exception {
        String body = TestClaims.validRequestJson(yesterday)
                .replace("dana@example.com", "not-an-email")
                .replace("HO-12345678", "12345")
                .replace("R3C 1A1", "90210");

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[0].field").value("address.postalCode"))
                .andExpect(jsonPath("$.errors[1].field").value("email"))
                .andExpect(jsonPath("$.errors[2].field").value("policyNumber"));
        verify(service, never()).submit(any(), any());
    }

    @Test
    void futureLossDateIsRejected() throws Exception {
        String body = TestClaims.validRequestJson(yesterday.plusDays(5));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("lossDate"));
    }

    @Test
    void unknownLossTypeIsAMalformedRequest() throws Exception {
        String body = TestClaims.validRequestJson(yesterday).replace("SEWER_BACKUP", "ALIENS");

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Malformed request"));
    }

    @Test
    void unexpectedErrorsReturn500WithoutLeakingInternals() throws Exception {
        when(service.submit(any(), any())).thenThrow(new IllegalStateException("db password is hunter2"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestClaims.validRequestJson(yesterday)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("We could not process your claim. Please try again later."))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(content().string(not(containsString("hunter2"))));
    }

    @Test
    void rejectedAndFailedSubmissionsAreNotCounted() throws Exception {
        double submittedBefore = meters.counter("claims.submitted").count();
        when(service.submit(any(), any())).thenThrow(new IllegalStateException("transaction rolled back"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(TestClaims.validRequestJson(yesterday)))
                .andExpect(status().isInternalServerError());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(meters.counter("claims.submitted").count()).isEqualTo(submittedBefore);
    }

    @Test
    void echoesASafeCorrelationIdAndReplacesAnUnsafeOne() throws Exception {
        when(service.submit(any(), any())).thenReturn(new SubmittedClaim("CLM-7K4Q-9XHM", ClaimStatus.SUBMITTED, Instant.now()));

        mvc.perform(post(URL).header("X-Correlation-Id", "abc-123")
                        .contentType(MediaType.APPLICATION_JSON).content(TestClaims.validRequestJson(yesterday)))
                .andExpect(header().string("X-Correlation-Id", "abc-123"));

        mvc.perform(post(URL).header("X-Correlation-Id", "bad\nINFO forged log line")
                        .contentType(MediaType.APPLICATION_JSON).content(TestClaims.validRequestJson(yesterday)))
                .andExpect(header().string("X-Correlation-Id", not(containsString("forged"))));
    }

    @Test
    void passesTheIdempotencyKeyThroughAndDoesNotCountAReplay() throws Exception {
        double submittedBefore = meters.counter("claims.submitted").count();
        when(service.submit(any(), eq("3f2b9c1e-key"))).thenReturn(
                new SubmittedClaim("CLM-7K4Q-9XHM", ClaimStatus.SUBMITTED, Instant.parse("2026-10-22T18:30:00Z"), true));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", "3f2b9c1e-key")
                        .content(TestClaims.validRequestJson(yesterday)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.claimNumber").value("CLM-7K4Q-9XHM"));

        assertThat(meters.counter("claims.submitted").count()).isEqualTo(submittedBefore);
    }

    @Test
    void aMalformedIdempotencyKeyIsRejected() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).header("Idempotency-Key", "not a valid key!")
                        .content(TestClaims.validRequestJson(yesterday)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Idempotency-Key"));

        verify(service, never()).submit(any(), any());
    }
}
