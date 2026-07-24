package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanApplication;
import com.dhruv.microloan_platform.entity.LoanProduct;
import com.dhruv.microloan_platform.entity.Repayment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class RepaymentRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;
    @Autowired
    private LoanProductRepository loanProductRepository;
    @Autowired
    private LoanApplicationRepository loanApplicationRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private RepaymentRepository repaymentRepository;

    private Long persistLoan() {
        Long borrowerId = borrowerRepository.save(Borrower.builder()
                .fullName("Test Borrower")
                .phone("9999999999")
                .email("repayment-repo-test@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000.00"))
                .build()).getId();

        Long productId = loanProductRepository.save(LoanProduct.builder()
                .name("Personal Loan")
                .minPrincipal(new BigDecimal("10000.00"))
                .maxPrincipal(new BigDecimal("500000.00"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .interestRate(new BigDecimal("12.50"))
                .penaltyRate(new BigDecimal("2.00"))
                .minKycLevel(KycLevel.BASIC)
                .build()).getId();

        Long applicationId = loanApplicationRepository.save(LoanApplication.builder()
                .borrowerId(borrowerId)
                .productId(productId)
                .requestedAmount(new BigDecimal("100000.00"))
                .requestedTenureMonths(12)
                .build()).getId();

        return loanRepository.save(Loan.builder()
                .borrowerId(borrowerId)
                .applicationId(applicationId)
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("12.50"))
                .penaltyRate(new BigDecimal("2.00"))
                .tenureMonths(12)
                .emiAmount(new BigDecimal("8884.88"))
                .totalPayable(new BigDecimal("106618.56"))
                .agreementSnapshot("{}")
                .build()).getId();
    }

    private Repayment newRepayment(Long loanId, String reference) {
        return Repayment.builder()
                .loanId(loanId)
                .amount(new BigDecimal("8884.88"))
                .paymentReference(reference)
                .paymentMode("UPI")
                .balanceAfter(new BigDecimal("97733.68"))
                .build();
    }

    @Test
    void savesAndFindsByPaymentReference() {
        Long loanId = persistLoan();
        repaymentRepository.save(newRepayment(loanId, "REF-1"));

        assertThat(repaymentRepository.findByPaymentReference("REF-1")).isPresent();
        assertThat(repaymentRepository.findByPaymentReference("NOPE")).isEmpty();
    }

    @Test
    void findsByLoanOrderedByPaidAt() throws InterruptedException {
        Long loanId = persistLoan();
        repaymentRepository.saveAndFlush(newRepayment(loanId, "REF-1"));
        Thread.sleep(5);
        repaymentRepository.saveAndFlush(newRepayment(loanId, "REF-2"));

        var repayments = repaymentRepository.findByLoanIdOrderByPaidAtAsc(loanId);

        assertThat(repayments).extracting(Repayment::getPaymentReference).containsExactly("REF-1", "REF-2");
    }

    @Test
    void rejectsDuplicatePaymentReference() {
        Long loanId = persistLoan();
        repaymentRepository.saveAndFlush(newRepayment(loanId, "DUPE"));

        assertThatThrownBy(() -> repaymentRepository.saveAndFlush(newRepayment(loanId, "DUPE")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
