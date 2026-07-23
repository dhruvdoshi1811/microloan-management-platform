package com.dhruv.microloan_platform.exception;

/**
 * Thrown when a requested entity (borrower, user, KYC record, ...) does not exist.
 * Mapped to HTTP 404 by {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
