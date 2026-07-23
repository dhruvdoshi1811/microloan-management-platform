package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.ApplicationStatus;
import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.LoanApplication;
import com.dhruv.microloan_platform.entity.LoanProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanApplicationRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;
    @Autowired
    private LoanProductRepository loanProductRepository;
    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    private Long persistBorrower() {
        Borrower borrower = Borrower.builder()
                .fullName("Test Borrower")
                .phone("9999999999")
                .email("loan-app-repo-test@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000.00"))
                .build();
        return borrowerRepository.save(borrower).getId();
    }

    private Long persistProduct() {
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
        return loanProductRepository.save(product).getId();
    }

    @Test
    void savesWithPendingStatusAndInitialVersion() {
        Long borrowerId = persistBorrower();
        Long productId = persistProduct();

        LoanApplication application = LoanApplication.builder()
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("100000.00"))
                .requestedTenureMonths(12)
                .build();

        LoanApplication saved = loanApplicationRepository.save(application);

        assertThat(saved.getStatus()).isEqualTo(ApplicationStatus.PENDING);
        assertThat(saved.getVersion()).isEqualTo(0L);
    }

    @Test
    void versionIncrementsOnEachUpdate() {
        Long borrowerId = persistBorrower();
        Long productId = persistProduct();

        LoanApplication application = LoanApplication.builder()
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("100000.00"))
                .requestedTenureMonths(12)
                .build();
        LoanApplication saved = loanApplicationRepository.saveAndFlush(application);
        assertThat(saved.getVersion()).isEqualTo(0L);

        saved.setStatus(ApplicationStatus.APPROVED);
        LoanApplication updated = loanApplicationRepository.saveAndFlush(saved);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    // A genuine two-concurrent-writers conflict needs two independent, overlapping
    // transactions (real threads, not just two finds in one @DataJpaTest transaction -
    // Hibernate's first-level cache collapses those into one managed instance, and a
    // detached-entity merge() reloads the current version rather than rejecting a stale
    // one, so neither approach actually reproduces a conflict here). That's exactly the
    // properly-threaded test Phase D builds for repayment processing; LoanApplicationServiceTest
    // .approvePropagatesOptimisticLockConflict already proves this service correctly surfaces
    // ObjectOptimisticLockingFailureException as a 409 when the repository throws it.
}
