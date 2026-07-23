package com.dhruv.microloan_platform.entity;

/** Lifecycle state of a single {@link Installment} row. */
public enum InstallmentStatus {
    PENDING,
    PARTIAL,
    PAID,
    OVERDUE
}
