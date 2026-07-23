package com.dhruv.microloan_platform.service.eligibility;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenureRangeRuleTest {

    private final TenureRangeRule rule = new TenureRangeRule();

    private LoanProduct product() {
        return LoanProduct.builder()
                .minPrincipal(new BigDecimal("10000"))
                .maxPrincipal(new BigDecimal("500000"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .interestRate(new BigDecimal("12"))
                .penaltyRate(new BigDecimal("2"))
                .minKycLevel(KycLevel.NONE)
                .build();
    }

    private Borrower borrower() {
        return Borrower.builder()
                .fullName("Alice")
                .phone("9999999999")
                .email("alice@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000"))
                .kycLevel(KycLevel.NONE)
                .build();
    }

    @Test
    void passesWhenWithinRange() {
        assertThatCode(() -> rule.check(product(), borrower(), new BigDecimal("100000"), 12))
                .doesNotThrowAnyException();
    }

    @Test
    void throwsWhenBelowMinimum() {
        assertThatThrownBy(() -> rule.check(product(), borrower(), new BigDecimal("100000"), 3))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void throwsWhenAboveMaximum() {
        assertThatThrownBy(() -> rule.check(product(), borrower(), new BigDecimal("100000"), 48))
                .isInstanceOf(BusinessRuleException.class);
    }
}
