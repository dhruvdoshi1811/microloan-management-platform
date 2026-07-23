package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
}
