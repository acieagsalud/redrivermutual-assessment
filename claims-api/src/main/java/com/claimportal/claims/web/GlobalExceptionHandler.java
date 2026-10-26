package com.claimportal.claims.web;

import com.claimportal.claims.observability.CorrelationId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;

/**
 * Every error leaves as RFC 9457 problem details. Clients get field-level messages for bad input and
 * a correlation id for anything else; stack traces and exception messages never reach them.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record FieldError(String field, String message) {
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldError::field))
                .toList();
        // Field names only: the rejected values may be personal information. A spike of these is worth seeing.
        log.info("Claim rejected by validation fields={}", errors.stream().map(FieldError::field).distinct().toList());
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Validation failed", "One or more fields are invalid.");
        problem.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        log.info("Claim rejected: request body could not be read");
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "Malformed request", "The request body could not be read."));
    }

    @ExceptionHandler(InvalidIdempotencyKeyException.class)
    public ResponseEntity<ProblemDetail> handleInvalidIdempotencyKey(InvalidIdempotencyKeyException ex) {
        log.info("Claim rejected: malformed Idempotency-Key header");
        return ResponseEntity.badRequest()
                .body(problem(HttpStatus.BAD_REQUEST, "Invalid Idempotency-Key", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        log.error("Unhandled error", ex);
        return ResponseEntity.internalServerError()
                .body(problem(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error",
                        "We could not process your claim. Please try again later."));
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        String correlationId = CorrelationId.current();
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}
