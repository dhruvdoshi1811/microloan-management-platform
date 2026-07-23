package com.dhruv.microloan_platform.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Reducing-balance EMI formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1), where r is the
 * monthly rate. Pure math, no Spring dependencies - shared by the eligibility rule engine
 * (Phase B) and the frozen agreement snapshot (Phase C), so the formula lives in exactly
 * one place.
 */
public final class EmiCalculator {

    private static final MathContext MC = new MathContext(12);

    private EmiCalculator() {
    }

    public static BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {
        BigDecimal monthlyRate = annualRatePercent.divide(BigDecimal.valueOf(1200), MC);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(tenureMonths), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal factor = onePlusR.pow(tenureMonths, MC);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(factor);
        BigDecimal denominator = factor.subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }
}
