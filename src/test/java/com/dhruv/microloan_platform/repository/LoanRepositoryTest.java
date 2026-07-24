package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanApplication;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.entity.LoanStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class LoanRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;
    @Autowired
    private LoanProductRepository loanProductRepository;
    @Autowired
    private LoanApplicationRepository loanApplicationRepository;
    @Autowired
    private LoanRepository loanRepository;

    private Long persistBorrower() {
        Borrower borrower = Borrower.builder()
                .fullName("Test Borrower")
                .phone("9999999999")
                .email("loan-repo-test@example.com")
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

    private Long persistApplication(Long borrowerId, Long productId) {
        LoanApplication application = LoanApplication.builder()
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("100000.00"))
                .requestedTenureMonths(12)
                .build();
        return loanApplicationRepository.save(application).getId();
    }

    private Loan newLoan(Long borrowerId, Long applicationId) {
        return Loan.builder()
                .borrowerId(borrowerId)
                .applicationId(applicationId)
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("12.50"))
                .penaltyRate(new BigDecimal("2.00"))
                .tenureMonths(12)
                .emiAmount(new BigDecimal("8930.11"))
                .totalPayable(new BigDecimal("107161.32"))
                .agreementSnapshot("{\"principal\":100000}")
                .build();
    }

    @Test
    void savesWithAgreementPendingDefaultsAndInitialVersion() {
        Long borrowerId = persistBorrower();
        Long applicationId = persistApplication(borrowerId, persistProduct());

        Loan saved = loanRepository.save(newLoan(borrowerId, applicationId));

        assertThat(saved.getStatus()).isEqualTo(LoanStatus.AGREEMENT_PENDING);
        assertThat(saved.getTotalPaid()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getVersion()).isEqualTo(0L);
        assertThat(saved.getAgreementAcknowledgedAt()).isNull();
        assertThat(saved.getDisbursedAt()).isNull();
    }

    @Test
    void rejectsSecondLoanForTheSameApplication() {
        Long borrowerId = persistBorrower();
        Long applicationId = persistApplication(borrowerId, persistProduct());

        loanRepository.saveAndFlush(newLoan(borrowerId, applicationId));

        assertThatThrownBy(() -> loanRepository.saveAndFlush(newLoan(borrowerId, applicationId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
