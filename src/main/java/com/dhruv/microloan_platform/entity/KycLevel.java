package com.dhruv.microloan_platform.entity;

/**
 * How much of a {@link Borrower}'s identity has been verified.
 * NONE -> BASIC once either PAN or Aadhaar is verified -> FULL once both are.
 * A later phase's LoanProduct rule engine gates loan eligibility on this level.
 */
public enum KycLevel {
    NONE,
    BASIC,
    FULL
}
