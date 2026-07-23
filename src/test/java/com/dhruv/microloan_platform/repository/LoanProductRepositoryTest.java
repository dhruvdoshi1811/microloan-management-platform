package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.LoanProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanProductRepositoryTest {

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Test
    void savesAndFindsWithDefaults() {
        LoanProduct product = LoanProduct.builder()
                .name("Personal Loan")
                .minPrincipal(new BigDecimal("10000.00"))
                .maxPrincipal(new BigDecimal("500000.00"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .interestRate(new BigDecimal("12.50"))
                .penaltyRate(new BigDecimal("2.00"))
                .minKycLevel(KycLevel.BASIC)
                .build();

        LoanProduct saved = loanProductRepository.save(product);
        LoanProduct found = loanProductRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.isActive()).isTrue();
        assertThat(found.getMinKycLevel()).isEqualTo(KycLevel.BASIC);
        assertThat(found.getCreatedAt()).isNotNull();
    }
}
