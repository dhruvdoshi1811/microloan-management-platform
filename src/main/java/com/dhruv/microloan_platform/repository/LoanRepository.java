package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Loan;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByApplicationId(Long applicationId);

    /**
     * SELECT ... FOR UPDATE on this one row. Used by RepaymentService to serialize
     * concurrent repayments against the same loan - see the class javadoc there for why
     * pessimistic (not @Version) locking is the right tool here.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);
}
