package com.dhruv.microloan_platform.entity;

/** Lifecycle state of a {@link LoanApplication}. Only PENDING can transition to the other two. */
public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
