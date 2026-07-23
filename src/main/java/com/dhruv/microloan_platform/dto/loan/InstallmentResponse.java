package com.dhruv.microloan_platform.dto.loan;

import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.InstallmentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InstallmentResponse(
        Long id,
        Long loanId,
        int installmentNo,
        LocalDate dueDate,
        BigDecimal emiAmount,
        BigDecimal penaltyAmount,
        BigDecimal totalDue,
        BigDecimal amountPaid,
        InstallmentStatus status,
        boolean penaltyApplied
) {

    public static InstallmentResponse from(Installment installment) {
        return new InstallmentResponse(
                installment.getId(),
                installment.getLoanId(),
                installment.getInstallmentNo(),
                installment.getDueDate(),
                installment.getEmiAmount(),
                installment.getPenaltyAmount(),
                installment.getTotalDue(),
                installment.getAmountPaid(),
                installment.getStatus(),
                installment.isPenaltyApplied());
    }
}
