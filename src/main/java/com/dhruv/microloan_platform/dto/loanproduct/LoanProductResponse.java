package com.dhruv.microloan_platform.dto.loanproduct;

import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.LoanProduct;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanProductResponse(
        Long id,
        String name,
        BigDecimal minPrincipal,
        BigDecimal maxPrincipal,
        int minTenureMonths,
        int maxTenureMonths,
        BigDecimal interestRate,
        BigDecimal penaltyRate,
        KycLevel minKycLevel,
        boolean isActive,
        Instant createdAt,
        Instant updatedAt
) {

    public static LoanProductResponse from(LoanProduct product) {
        return new LoanProductResponse(
                product.getId(),
                product.getName(),
                product.getMinPrincipal(),
                product.getMaxPrincipal(),
                product.getMinTenureMonths(),
                product.getMaxTenureMonths(),
                product.getInterestRate(),
                product.getPenaltyRate(),
                product.getMinKycLevel(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
