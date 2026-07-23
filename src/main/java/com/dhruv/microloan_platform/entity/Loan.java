package com.dhruv.microloan_platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Created the moment a LoanApplication is approved, in AGREEMENT_PENDING status.
 * {@code agreementSnapshot} is a JSON string frozen at that instant (principal, rate,
 * tenure, computed EMI) - even if the originating LoanProduct's rate changes later, this
 * loan's terms never silently change, since nothing here re-reads LoanProduct after
 * creation. Plain Long FKs (borrowerId, applicationId), same convention as every other
 * entity in this codebase.
 */
@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Column(name = "application_id", nullable = false, unique = true)
    private Long applicationId;

    @Column(name = "principal_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "tenure_months", nullable = false)
    private int tenureMonths;

    @Column(name = "emi_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal emiAmount;

    @Column(name = "total_payable", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPayable;

    @Column(name = "total_paid", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private LoanStatus status = LoanStatus.AGREEMENT_PENDING;

    @Column(name = "agreement_snapshot", nullable = false, columnDefinition = "TEXT")
    private String agreementSnapshot;

    @Column(name = "agreement_acknowledged_at")
    private Instant agreementAcknowledgedAt;

    @Column(name = "disbursed_at")
    private Instant disbursedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
