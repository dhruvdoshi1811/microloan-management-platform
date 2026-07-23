package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.DocumentType;
import com.dhruv.microloan_platform.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /** The most recently issued OTP challenge for this borrower + document, if any. */
    Optional<OtpVerification> findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(Long borrowerId, DocumentType documentType);
}
