package com.dhruv.microloan_platform.exception;

/**
 * Thrown when a create operation would violate a business uniqueness rule
 * (duplicate email, PAN, Aadhaar, ...). Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
