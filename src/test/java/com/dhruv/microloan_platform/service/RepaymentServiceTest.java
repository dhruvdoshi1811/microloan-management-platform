package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.repayment.RepaymentRequest;
import com.dhruv.microloan_platform.dto.repayment.RepaymentResponse;
import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanStatus;
import com.dhruv.microloan_platform.entity.Repayment;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.InstallmentRepository;
import com.dhruv.microloan_platform.repository.LoanRepository;
import com.dhruv.microloan_platform.repository.RepaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock
    private RepaymentRepository repaymentRepository;
    @Mock
    private LoanRepository loanRepository;
    @Mock
    private InstallmentRepository installmentRepository;
    @Mock
    private OutboxEventWriter outboxEventWriter;

    @InjectMocks
    private RepaymentService repaymentService;

    private static final Long LOAN_ID = 1L;

    private Loan activeLoan() {
        return Loan.builder()
                .id(LOAN_ID)
                .borrowerId(2L)
                .applicationId(3L)
                .principalAmount(new BigDecimal("200000.00"))
                .interestRate(new BigDecimal("12.00"))
                .tenureMonths(10)
                .emiAmount(new BigDecimal("20000.00"))
                .totalPayable(new BigDecimal("200000.00"))
                .totalPaid(BigDecimal.ZERO)
                .agreementSnapshot("{}")
                .status(LoanStatus.ACTIVE)
                .build();
    }

    private Installment pendingInstallment(int no) {
        return Installment.builder()
                .id((long) no)
                .loanId(LOAN_ID)
                .installmentNo(no)
                .dueDate(java.time.LocalDate.now().plusMonths(no))
                .emiAmount(new BigDecimal("20000.00"))
                .totalDue(new BigDecimal("20000.00"))
                .build();
    }

    private RepaymentRequest request(BigDecimal amount, String reference) {
        return new RepaymentRequest(LOAN_ID, amount, reference, "UPI");
    }

    private void stubNoExistingRepayment(String reference) {
        when(repaymentRepository.findByPaymentReference(reference)).thenReturn(Optional.empty());
    }

    private void stubSaves() {
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repaymentRepository.save(any(Repayment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void exactPaymentMarksSingleInstallmentPaid() {
        Loan loan = activeLoan();
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        stubNoExistingRepayment("REF-1");
        List<Installment> installments = List.of(pendingInstallment(1), pendingInstallment(2));
        when(installmentRepository.findUnpaidByLoanIdForUpdate(LOAN_ID, InstallmentStatus.PAID)).thenReturn(installments);
        stubSaves();

        RepaymentResponse response = repaymentService.processRepayment(request(new BigDecimal("20000.00"), "REF-1"));

        assertThat(installments.get(0).getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(installments.get(0).getAmountPaid()).isEqualByComparingTo("20000.00");
        assertThat(installments.get(1).getStatus()).isEqualTo(InstallmentStatus.PENDING);
        assertThat(loan.getTotalPaid()).isEqualByComparingTo("20000.00");
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.balanceAfter()).isEqualByComparingTo("180000.00");

        verify(outboxEventWriter).write(eq("LOAN"), eq(LOAN_ID), eq("REPAYMENT_RECEIVED"), any());
    }

    @Test
    void paymentSpanningTwoInstallmentsLeavesSecondPartial() {
        Loan loan = activeLoan();
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        stubNoExistingRepayment("REF-1");
        List<Installment> installments = List.of(pendingInstallment(1), pendingInstallment(2));
        when(installmentRepository.findUnpaidByLoanIdForUpdate(LOAN_ID, InstallmentStatus.PAID)).thenReturn(installments);
        stubSaves();

        repaymentService.processRepayment(request(new BigDecimal("30000.00"), "REF-1"));

        assertThat(installments.get(0).getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(installments.get(0).getAmountPaid()).isEqualByComparingTo("20000.00");
        assertThat(installments.get(1).getStatus()).isEqualTo(InstallmentStatus.PARTIAL);
        assertThat(installments.get(1).getAmountPaid()).isEqualByComparingTo("10000.00");
        assertThat(loan.getTotalPaid()).isEqualByComparingTo("30000.00");
    }

    @Test
    void payingOffFullOutstandingClosesLoan() {
        Loan loan = activeLoan();
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        stubNoExistingRepayment("REF-1");
        List<Installment> installments = List.of(pendingInstallment(1));
        when(installmentRepository.findUnpaidByLoanIdForUpdate(LOAN_ID, InstallmentStatus.PAID)).thenReturn(installments);
        stubSaves();
        loan.setTotalPayable(new BigDecimal("20000.00"));

        repaymentService.processRepayment(request(new BigDecimal("20000.00"), "REF-1"));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
    }

    @Test
    void idempotentReplayReturnsExistingWithoutReprocessing() {
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(activeLoan()));
        Repayment existing = Repayment.builder()
                .id(99L).loanId(LOAN_ID).amount(new BigDecimal("20000.00"))
                .paymentReference("REF-1").paymentMode("UPI").balanceAfter(new BigDecimal("180000.00"))
                .build();
        when(repaymentRepository.findByPaymentReference("REF-1")).thenReturn(Optional.of(existing));

        RepaymentResponse response = repaymentService.processRepayment(request(new BigDecimal("20000.00"), "REF-1"));

        assertThat(response.id()).isEqualTo(99L);
        verify(installmentRepository, never()).findUnpaidByLoanIdForUpdate(any(), any());
        verify(installmentRepository, never()).saveAll(anyList());
        verify(loanRepository, never()).save(any());
        verify(repaymentRepository, never()).save(any());
        verify(outboxEventWriter, never()).write(any(), any(), any(), any());
    }

    @Test
    void rejectsAmountExceedingOutstandingBalance() {
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(activeLoan()));
        stubNoExistingRepayment("REF-1");

        assertThatThrownBy(() -> repaymentService.processRepayment(request(new BigDecimal("250000.00"), "REF-1")))
                .isInstanceOf(BusinessRuleException.class);

        verify(installmentRepository, never()).findUnpaidByLoanIdForUpdate(any(), any());
    }

    @Test
    void rejectsRepaymentOnAgreementPendingLoan() {
        Loan loan = activeLoan();
        loan.setStatus(LoanStatus.AGREEMENT_PENDING);
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        stubNoExistingRepayment("REF-1");

        assertThatThrownBy(() -> repaymentService.processRepayment(request(new BigDecimal("20000.00"), "REF-1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsRepaymentOnClosedLoan() {
        Loan loan = activeLoan();
        loan.setStatus(LoanStatus.CLOSED);
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        stubNoExistingRepayment("REF-1");

        assertThatThrownBy(() -> repaymentService.processRepayment(request(new BigDecimal("20000.00"), "REF-1")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void allowsRepaymentOnOverdueLoan() {
        Loan loan = activeLoan();
        loan.setStatus(LoanStatus.OVERDUE);
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.of(loan));
        stubNoExistingRepayment("REF-1");
        List<Installment> installments = List.of(pendingInstallment(1));
        when(installmentRepository.findUnpaidByLoanIdForUpdate(LOAN_ID, InstallmentStatus.PAID)).thenReturn(installments);
        stubSaves();

        RepaymentResponse response = repaymentService.processRepayment(request(new BigDecimal("20000.00"), "REF-1"));

        assertThat(response).isNotNull();
    }

    @Test
    void processRepaymentThrowsWhenLoanMissing() {
        when(loanRepository.findByIdForUpdate(LOAN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repaymentService.processRepayment(request(new BigDecimal("20000.00"), "REF-1")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repaymentRepository, never()).findByPaymentReference(any());
    }

    @Test
    void getThrowsWhenMissing() {
        when(repaymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> repaymentService.get(404L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByLoanThrowsWhenLoanMissing() {
        when(loanRepository.existsById(404L)).thenReturn(false);

        assertThatThrownBy(() -> repaymentService.getByLoan(404L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
