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

class KycLevelRuleTest {

    private final KycLevelRule rule = new KycLevelRule();

    private LoanProduct productRequiring(KycLevel minKycLevel) {
        return LoanProduct.builder()
                .minPrincipal(new BigDecimal("10000"))
                .maxPrincipal(new BigDecimal("500000"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .interestRate(new BigDecimal("12"))
                .penaltyRate(new BigDecimal("2"))
                .minKycLevel(minKycLevel)
                .build();
    }

    private Borrower borrowerWith(KycLevel kycLevel) {
        return Borrower.builder()
                .fullName("Alice")
                .phone("9999999999")
                .email("alice@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000"))
                .kycLevel(kycLevel)
                .build();
    }

    @Test
    void passesWhenBorrowerMeetsExactMinimum() {
        assertThatCode(() -> rule.check(productRequiring(KycLevel.BASIC), borrowerWith(KycLevel.BASIC),
                new BigDecimal("100000"), 12))
                .doesNotThrowAnyException();
    }

    @Test
    void passesWhenBorrowerExceedsMinimum() {
        assertThatCode(() -> rule.check(productRequiring(KycLevel.BASIC), borrowerWith(KycLevel.FULL),
                new BigDecimal("100000"), 12))
                .doesNotThrowAnyException();
    }

    @Test
    void throwsWhenBorrowerBelowMinimum() {
        assertThatThrownBy(() -> rule.check(productRequiring(KycLevel.FULL), borrowerWith(KycLevel.BASIC),
                new BigDecimal("100000"), 12))
                .isInstanceOf(BusinessRuleException.class);
    }
}
