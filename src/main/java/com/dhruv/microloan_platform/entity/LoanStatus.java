package com.dhruv.microloan_platform.entity;

/**
 * Lifecycle state of a {@link Loan}. AGREEMENT_PENDING (just approved, snapshot frozen) ->
 * ACTIVE (borrower acknowledged, installment schedule generated, disbursed) -> OVERDUE
 * (a later phase's batch job flags this) -> CLOSED (fully repaid).
 */
public enum LoanStatus {
    AGREEMENT_PENDING,
    ACTIVE,
    OVERDUE,
    CLOSED
}
