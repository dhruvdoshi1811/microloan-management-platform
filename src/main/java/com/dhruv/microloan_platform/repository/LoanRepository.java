package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {
}
