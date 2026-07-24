package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Loan;
import com.dhruv.microloan_platform.entity.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByApplicationId(Long applicationId);

    /**
     * Used by OverdueService to page through active/overdue loans - not locked itself, each
     * loan is locked individually as it's processed. Explicit order (rather than leaving page
     * boundaries to whatever order the DB happens to return) keeps pagination stable even if
     * a status changes mid-scan - this job only ever moves a loan ACTIVE -> OVERDUE, which
     * stays inside this same filter, so a stable sort is enough to avoid skipped/duplicated rows.
     */
    Page<Loan> findByStatusInOrderByIdAsc(Collection<LoanStatus> statuses, Pageable pageable);

    /**
     * SELECT ... FOR UPDATE on this one row. Used by RepaymentService to serialize
     * concurrent repayments against the same loan - see the class javadoc there for why
     * pessimistic (not @Version) locking is the right tool here.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Loan l where l.id = :id")
    Optional<Loan> findByIdForUpdate(@Param("id") Long id);
}
