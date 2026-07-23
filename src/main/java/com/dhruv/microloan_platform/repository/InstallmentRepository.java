package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Installment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByLoanIdOrderByInstallmentNoAsc(Long loanId);
}
