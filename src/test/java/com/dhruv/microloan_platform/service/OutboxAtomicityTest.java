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

/**
 * The proof for "no event lost on crash": the real RepaymentService, with OutboxEventWriter
 * replaced by a mock stubbed to throw. This forces a real failure partway through
 * processRepayment's transaction - Spring rolls the whole thing back on the unchecked
 * exception, exactly as a real crash mid-transaction would. If the state change and its
 * outbox row weren't genuinely atomic, this test would find a Loan with an updated
 * totalPaid but no Repayment row (or the reverse) after the "crash." It won't, because
 * @Transactional wraps the state change and the outbox write in one unit.
 */
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
        // Setup succeeds even with OutboxEventWriter mocked - its write() calls during
        // approve() are harmless no-ops until stubbed below.
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
