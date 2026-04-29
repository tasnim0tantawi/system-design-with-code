package com.booking.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * RFC 7807 (application/problem+json) responses for every error.
 * Each response includes: type, title, status, detail, instance, plus
 * extension members `code` (machine-readable) and `timestamp`.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI BASE_TYPE = URI.create("https://api.booking.com/problems/");

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApi(ApiException ex, ServerHttpRequest req) {
        return build(ex.getStatus(), ex.getCode(), titleFor(ex.getStatus()), ex.getMessage(), req, null);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ProblemDetail handleValidation(WebExchangeBindException ex, ServerHttpRequest req) {
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> Map.of(
                        "field", fe.getField(),
                        "message", fe.getDefaultMessage() == null ? "invalid" : fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation Failed",
                "Request validation failed", req, errors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegal(IllegalArgumentException ex, ServerHttpRequest req) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Bad Request", ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleAll(Exception ex, ServerHttpRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal Server Error",
                ex.getMessage() == null ? "An unexpected error occurred" : ex.getMessage(), req, null);
    }

    private ProblemDetail build(HttpStatus status, String code, String title, String detail,
                                ServerHttpRequest req, List<Map<String, String>> errors) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setType(BASE_TYPE.resolve(code.toLowerCase().replace('_', '-')));
        pd.setTitle(title);
        pd.setInstance(URI.create(req.getURI().getPath()));
        pd.setProperty("code", code);
        pd.setProperty("timestamp", Instant.now().toString());
        if (errors != null) {
            pd.setProperty("errors", errors);
        }
        return pd;
    }

    private String titleFor(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Resource Not Found";
            case BAD_REQUEST -> "Bad Request";
            case FORBIDDEN -> "Forbidden";
            case CONFLICT -> "Conflict";
            case UNAUTHORIZED -> "Unauthorized";
            default -> status.getReasonPhrase();
        };
    }
}
