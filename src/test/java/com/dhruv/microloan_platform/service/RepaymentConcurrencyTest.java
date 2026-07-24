package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.borrower.BorrowerRequest;
import com.dhruv.microloan_platform.dto.loan.InstallmentResponse;
import com.dhruv.microloan_platform.dto.loan.LoanResponse;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentResponse;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.repository.LoanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RepaymentConcurrencyTest {

    @Autowired
    private BorrowerService borrowerService;
    @Autowired
    private LoanProductService loanProductService;
    @Autowired
    private LoanApplicationService loanApplicationService;
    @Autowired
    private LoanService loanService;
    @Autowired
    private RepaymentService repaymentService;
    @Autowired
    private LoanRepository loanRepository;

    @Test
    void twoConcurrentRepaymentsOnTheSameLoanBothAllocateCorrectly() throws Exception {
        Long borrowerId = borrowerService.create(new BorrowerRequest(
                "Concurrency Borrower", "9999999999", "concurrency-test@example.com",
                LocalDate.of(1990, 1, 1), new BigDecimal("1000000.00"))).id();

        Long productId = loanProductService.create(new LoanProductRequest(
                "Concurrency Test Product", new BigDecimal("10000.00"), new BigDecimal("500000.00"),
                6, 36, new BigDecimal("12.00"), new BigDecimal("2.00"), KycLevel.NONE)).id();

        Long applicationId = loanApplicationService.submit(new LoanApplicationRequest(
                borrowerId, productId, new BigDecimal("120000.00"), 12)).id();

        loanApplicationService.approve(applicationId);
        Long loanId = loanRepository.findByApplicationId(applicationId).orElseThrow().getId();

        LoanResponse activatedLoan = loanService.acknowledgeAgreement(loanId);
        BigDecimal emi = activatedLoan.emiAmount();
        BigDecimal totalPayable = activatedLoan.totalPayable();

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RepaymentResponse> resultA = executor.submit(() -> {
                barrier.await();
                return repaymentService.processRepayment(new RepaymentRequest(loanId, emi, "REF-A", "UPI"));
            });
            Future<RepaymentResponse> resultB = executor.submit(() -> {
                barrier.await();
                return repaymentService.processRepayment(new RepaymentRequest(loanId, emi, "REF-B", "UPI"));
            });

            RepaymentResponse responseA = resultA.get(10, TimeUnit.SECONDS);
            RepaymentResponse responseB = resultB.get(10, TimeUnit.SECONDS);

            assertThat(responseA.amount()).isEqualByComparingTo(emi);
            assertThat(responseB.amount()).isEqualByComparingTo(emi);

            LoanResponse finalLoan = loanService.get(loanId);
            assertThat(finalLoan.totalPaid()).isEqualByComparingTo(emi.multiply(BigDecimal.valueOf(2)));

            List<InstallmentResponse> installments = loanService.getInstallments(loanId);
            assertThat(installments.get(0).status()).isEqualTo(InstallmentStatus.PAID);
            assertThat(installments.get(1).status()).isEqualTo(InstallmentStatus.PAID);
            assertThat(installments.get(2).status()).isEqualTo(InstallmentStatus.PENDING);

            List<RepaymentResponse> repayments = repaymentService.getByLoan(loanId);
            assertThat(repayments).hasSize(2);
            assertThat(repayments).extracting(RepaymentResponse::paymentReference)
                    .containsExactlyInAnyOrder("REF-A", "REF-B");

            List<BigDecimal> balancesAfter = repayments.stream().map(RepaymentResponse::balanceAfter).toList();
            assertThat(balancesAfter).satisfiesExactlyInAnyOrder(
                    balance -> assertThat(balance).isEqualByComparingTo(totalPayable.subtract(emi)),
                    balance -> assertThat(balance).isEqualByComparingTo(totalPayable.subtract(emi.multiply(BigDecimal.valueOf(2)))));
        } finally {
            executor.shutdownNow();
        }
    }
}
