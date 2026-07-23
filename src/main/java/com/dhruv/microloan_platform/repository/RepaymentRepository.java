package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepaymentRepository extends JpaRepository<Repayment, Long> {

    Optional<Repayment> findByPaymentReference(String paymentReference);

    List<Repayment> findByLoanIdOrderByPaidAtAsc(Long loanId);
}
