package com.dhruv.microloan_platform.exception;

/**
 * Thrown when a request is well-formed but violates a domain rule
 * (OTP expired, OTP attempts exhausted, wrong OTP code, ...).
 * Mapped to HTTP 422 by {@link GlobalExceptionHandler}.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
