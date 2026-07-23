package com.dhruv.microloan_platform.entity;

/**
 * Authorization role for a {@link User}. Drives which endpoints are permitted -
 * ADMIN manages loan products and the admin/observability endpoints,
 * BORROWER is a self-service login (linked to a {@link Borrower} profile in a later phase).
 */
public enum Role {
    ADMIN,
    BORROWER
}
