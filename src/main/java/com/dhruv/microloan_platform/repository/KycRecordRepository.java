package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.KycRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KycRecordRepository extends JpaRepository<KycRecord, Long> {

    Optional<KycRecord> findByBorrowerId(Long borrowerId);

    boolean existsByPanNumber(String panNumber);

    boolean existsByAadhaarNumber(String aadhaarNumber);
}
