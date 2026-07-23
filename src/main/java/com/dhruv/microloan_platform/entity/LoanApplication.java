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
 * A borrower's request against a specific LoanProduct. borrowerId/productId are plain FK
 * columns, not JPA relationships - same convention as KycRecord/OtpVerification.
 * {@code version} is optimistic locking: two concurrent approve/reject calls on the same
 * PENDING row will have the second save() throw ObjectOptimisticLockingFailureException
 * (mapped to 409 by GlobalExceptionHandler) rather than silently double-processing.
 */
@Entity
@Table(name = "loan_applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrower_id", nullable = false)
    private Long borrowerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "requested_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "requested_tenure_months", nullable = false)
    private int requestedTenureMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

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
