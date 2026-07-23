package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real rule instances, no mocks - these rules are pure logic, so this exercises the actual
 * engine wiring (list order = check order = fail-fast) rather than re-testing each rule's
 * internals, which the per-rule test classes already cover.
 */
class LoanEligibilityServiceTest {

    private final LoanEligibilityService engine = new LoanEligibilityService(List.of(
            new PrincipalRangeRule(), new TenureRangeRule(), new KycLevelRule(), new EmiToIncomeRatioRule()));

    private LoanProduct product() {
        return LoanProduct.builder()
                .minPrincipal(new BigDecimal("10000"))
                .maxPrincipal(new BigDecimal("500000"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .interestRate(new BigDecimal("12"))
                .penaltyRate(new BigDecimal("2"))
                .minKycLevel(KycLevel.BASIC)
                .build();
    }

    private Borrower borrower() {
        return Borrower.builder()
                .fullName("Alice")
                .phone("9999999999")
                .email("alice@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000"))
                .kycLevel(KycLevel.FULL)
                .build();
    }

    @Test
    void passesWhenEveryRulePasses() {
        assertThatCode(() -> engine.checkEligibility(product(), borrower(), new BigDecimal("100000"), 12))
                .doesNotThrowAnyException();
    }

    @Test
    void stopsAtFirstFailingRuleRatherThanCheckingAll() {
        // Violates both PrincipalRangeRule (too low) and TenureRangeRule (too short) -
        // since PrincipalRangeRule is first in the list, its message should win.
        assertThatThrownBy(() -> engine.checkEligibility(product(), borrower(), new BigDecimal("100"), 1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Requested amount");
    }

    @Test
    void kycFailureSurfacesWhenRangeChecksPass() {
        Borrower unverifiedBorrower = Borrower.builder()
                .fullName("Bob")
                .phone("8888888888")
                .email("bob@example.com")
                .dob(LocalDate.of(1990, 1, 1))
                .monthlyIncome(new BigDecimal("50000"))
                .kycLevel(KycLevel.NONE)
                .build();

        assertThatThrownBy(() -> engine.checkEligibility(product(), unverifiedBorrower, new BigDecimal("100000"), 12))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("KYC level");
    }
}
