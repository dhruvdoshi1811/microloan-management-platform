package com.dhruv.microloan_platform.entity;

/**
 * How much of a {@link Borrower}'s identity has been verified.
 * NONE -> BASIC once either PAN or Aadhaar is verified -> FULL once both are.
 * The LoanProduct rule engine gates loan eligibility on this level.
 */
public enum KycLevel {
    NONE,
    BASIC,
    FULL;

    /**
     * True if this level satisfies a required minimum, relying on declaration order
     * (NONE < BASIC < FULL) rather than a separately-maintained rank.
     */
    public boolean atLeast(KycLevel required) {
        return this.ordinal() >= required.ordinal();
    }
}
