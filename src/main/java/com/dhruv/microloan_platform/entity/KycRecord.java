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
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * One row per borrower, tracking their PAN/Aadhaar numbers and whether each has been
 * OTP-verified. {@code borrowerId} is a plain FK column, not a JPA relationship - see
 * Phase A plan notes for why (matches the spec's explicit-FK style throughout).
 */
@Entity
@Table(name = "kyc_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "borrower_id", nullable = false, unique = true)
    private Long borrowerId;

    @Column(name = "pan_number", length = 10, unique = true)
    private String panNumber;

    @Column(name = "aadhaar_number", length = 12, unique = true)
    private String aadhaarNumber;

    @Column(name = "pan_verified", nullable = false)
    @Builder.Default
    private boolean panVerified = false;

    @Column(name = "aadhaar_verified", nullable = false)
    @Builder.Default
    private boolean aadhaarVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
