package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, Long> {
}
