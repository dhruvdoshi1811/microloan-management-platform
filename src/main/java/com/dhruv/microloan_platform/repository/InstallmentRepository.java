package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Installment;
import com.dhruv.microloan_platform.entity.InstallmentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findByLoanIdOrderByInstallmentNoAsc(Long loanId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Installment i where i.loanId = :loanId and i.status <> :paidStatus order by i.installmentNo asc")
    List<Installment> findUnpaidByLoanIdForUpdate(@Param("loanId") Long loanId, @Param("paidStatus") InstallmentStatus paidStatus);
}
