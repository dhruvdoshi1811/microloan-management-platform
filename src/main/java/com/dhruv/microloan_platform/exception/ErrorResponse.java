package com.dhruv.microloan_platform.exception;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned by every failed request. {@code fieldErrors} is only
 * populated for {@code @Valid} validation failures (field name -> message); null otherwise.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse ofValidation(int status, String error, String message, String path,
                                              Map<String, String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
