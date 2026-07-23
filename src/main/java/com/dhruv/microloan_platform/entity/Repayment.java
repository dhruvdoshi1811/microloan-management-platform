package com.dhruv.microloan_platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One row per processed repayment - an append-only ledger entry, never edited after
 * creation (unlike every other entity here, there's no updatedAt). paymentReference is
 * unique and doubles as the idempotency key: RepaymentService checks it before allocating
 * anything, so a retried request with the same reference returns the original result
 * instead of double-processing.
 */
@Entity
@Table(name = "repayments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 100)
    private String paymentReference;

    @Column(name = "payment_mode", nullable = false, length = 30)
    private String paymentMode;

    @Column(name = "balance_after", nullable = false, precision = 14, scale = 2)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(name = "paid_at", nullable = false, updatable = false)
    private Instant paidAt;
}
