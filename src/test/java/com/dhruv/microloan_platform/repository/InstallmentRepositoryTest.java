package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanApplication;
import com.dhruv.microloan_platform.entity.LoanProduct;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class InstallmentRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;
    @Autowired
    private LoanProductRepository loanProductRepository;
    @Autowired
    private LoanApplicationRepository loanApplicationRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private InstallmentRepository installmentRepository;

    private Long persistLoan() {
        Long borrowerId = borrowerRepository.save(Borrower.builder()
                .fullName("Test Borrower")
                .phone("9999999999")
                .email("installment-repo-test@example.com")
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
                .requestedTenureMonths(3)
                .build()).getId();

        return loanRepository.save(Loan.builder()
                .borrowerId(borrowerId)
                .applicationId(applicationId)
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("12.50"))
                .penaltyRate(new BigDecimal("2.00"))
                .tenureMonths(3)
                .emiAmount(new BigDecimal("34000.00"))
                .totalPayable(new BigDecimal("102000.00"))
                .agreementSnapshot("{\"principal\":100000}")
                .build()).getId();
    }

    @Test
    void findsInstallmentsOrderedByInstallmentNo() {
        Long loanId = persistLoan();
        installmentRepository.save(installment(loanId, 2));
        installmentRepository.save(installment(loanId, 1));
        installmentRepository.save(installment(loanId, 3));

        List<Installment> installments = installmentRepository.findByLoanIdOrderByInstallmentNoAsc(loanId);

        assertThat(installments).extracting(Installment::getInstallmentNo).containsExactly(1, 2, 3);
    }

    private Installment installment(Long loanId, int installmentNo) {
        return Installment.builder()
                .loanId(loanId)
                .installmentNo(installmentNo)
                .dueDate(LocalDate.now().plusMonths(installmentNo))
                .emiAmount(new BigDecimal("34000.00"))
                .totalDue(new BigDecimal("34000.00"))
                .build();
    }
}
