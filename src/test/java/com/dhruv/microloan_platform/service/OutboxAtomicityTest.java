package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.borrower.BorrowerRequest;
import com.dhruv.microloan_platform.dto.loan.LoanResponse;
import com.dhruv.microloan_platform.dto.loanapplication.LoanApplicationRequest;
import com.dhruv.microloan_platform.dto.loanproduct.LoanProductRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentRequest;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.repository.LoanRepository;
import com.dhruv.microloan_platform.repository.RepaymentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
class OutboxAtomicityTest {

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
    @Autowired
    private RepaymentRepository repaymentRepository;

    @MockitoBean
    private OutboxEventWriter outboxEventWriter;

    @Test
    void repaymentIsFullyRolledBackWhenTheOutboxWriteFails() {
        Long borrowerId = borrowerService.create(new BorrowerRequest(
                "Atomicity Borrower", "9999999999", "atomicity-test@example.com",
                LocalDate.of(1990, 1, 1), new BigDecimal("1000000.00"))).id();

        Long productId = loanProductService.create(new LoanProductRequest(
                "Atomicity Test Product", new BigDecimal("10000.00"), new BigDecimal("500000.00"),
                6, 36, new BigDecimal("12.00"), new BigDecimal("2.00"), KycLevel.NONE)).id();

        Long applicationId = loanApplicationService.submit(new LoanApplicationRequest(
                borrowerId, productId, new BigDecimal("120000.00"), 12)).id();

        loanApplicationService.approve(applicationId);
        Long loanId = loanRepository.findByApplicationId(applicationId).orElseThrow().getId();
        LoanResponse activatedLoan = loanService.acknowledgeAgreement(loanId);

        BigDecimal totalPaidBefore = loanService.get(loanId).totalPaid();

        doThrow(new RuntimeException("simulated crash mid-transaction"))
                .when(outboxEventWriter).write(any(), any(), any(), any());

        RepaymentRequest request = new RepaymentRequest(loanId, activatedLoan.emiAmount(), "ATOMICITY-REF", "UPI");

        assertThatThrownBy(() -> repaymentService.processRepayment(request))
                .isInstanceOf(RuntimeException.class);

        LoanResponse afterFailedAttempt = loanService.get(loanId);
        assertThat(afterFailedAttempt.totalPaid()).isEqualByComparingTo(totalPaidBefore);
        assertThat(repaymentRepository.findByPaymentReference("ATOMICITY-REF")).isEmpty();
    }
}
