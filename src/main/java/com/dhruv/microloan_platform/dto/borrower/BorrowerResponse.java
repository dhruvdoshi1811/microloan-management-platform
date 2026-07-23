package com.dhruv.microloan_platform.dto.borrower;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BorrowerResponse(
        Long id,
        String fullName,
        String phone,
        String email,
        LocalDate dob,
        BigDecimal monthlyIncome,
        KycLevel kycLevel,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {

    public static BorrowerResponse from(Borrower borrower) {
        return new BorrowerResponse(
                borrower.getId(),
                borrower.getFullName(),
                borrower.getPhone(),
                borrower.getEmail(),
                borrower.getDob(),
                borrower.getMonthlyIncome(),
                borrower.getKycLevel(),
                borrower.isActive(),
                borrower.getCreatedAt(),
                borrower.getUpdatedAt());
    }
}
