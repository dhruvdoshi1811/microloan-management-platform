package com.dhruv.microloan_platform.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EmiCalculatorTest {

    @Test
    void calculatesEmiForAKnownExample() {
        BigDecimal emi = EmiCalculator.calculateEmi(
                new BigDecimal("100000"), new BigDecimal("12"), 12);

        assertThat(emi).isEqualByComparingTo("8884.88");
    }

    @Test
    void zeroInterestRateDividesPrincipalEvenlyAcrossTenure() {
        BigDecimal emi = EmiCalculator.calculateEmi(
                new BigDecimal("120000"), BigDecimal.ZERO, 12);

        assertThat(emi).isEqualByComparingTo("10000.00");
    }

    @Test
    void higherPrincipalProducesHigherEmiForSameRateAndTenure() {
        BigDecimal smaller = EmiCalculator.calculateEmi(new BigDecimal("50000"), new BigDecimal("10"), 24);
        BigDecimal larger = EmiCalculator.calculateEmi(new BigDecimal("100000"), new BigDecimal("10"), 24);

        assertThat(larger).isGreaterThan(smaller);
    }
}
