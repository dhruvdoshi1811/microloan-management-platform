package com.dhruv.microloan_platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * The configurable constraints an admin sets per loan product - principal/tenure bounds,
 * rates, and the minimum KycLevel a borrower needs. The eligibility rule engine
 * (service/eligibility) reads these fields instead of hardcoding limits in code.
 */
@Entity
@Table(name = "loan_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "min_principal", nullable = false, precision = 14, scale = 2)
    private BigDecimal minPrincipal;

    @Column(name = "max_principal", nullable = false, precision = 14, scale = 2)
    private BigDecimal maxPrincipal;

    @Column(name = "min_tenure_months", nullable = false)
    private int minTenureMonths;

    @Column(name = "max_tenure_months", nullable = false)
    private int maxTenureMonths;

    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(name = "penalty_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal penaltyRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "min_kyc_level", nullable = false, length = 10)
    private KycLevel minKycLevel;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
