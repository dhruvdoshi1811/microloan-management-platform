package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.kyc.KycInitiateRequest;
import com.dhruv.microloan_platform.dto.kyc.KycResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpInitiateResponse;
import com.dhruv.microloan_platform.dto.kyc.OtpVerifyRequest;
import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.DocumentType;
import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.KycRecord;
import com.dhruv.microloan_platform.entity.OtpVerification;
import com.dhruv.microloan_platform.exception.BusinessRuleException;
import com.dhruv.microloan_platform.exception.DuplicateResourceException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.BorrowerRepository;
import com.dhruv.microloan_platform.repository.KycRecordRepository;
import com.dhruv.microloan_platform.repository.OtpVerificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Owns the KYC lifecycle: initiate (register a PAN/Aadhaar number, issue an OTP) and
 * verify-otp (check the code, flip the verified flag, recompute the borrower's KycLevel).
 * There's no real SMS/UIDAI gateway here - see {@link OtpInitiateResponse} for why the code
 * comes back in the response instead of being "sent" anywhere.
 */
@Service
public class KycService {

    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_TTL = Duration.ofMinutes(5);
    private static final int MAX_ATTEMPTS = 5;

    private final BorrowerRepository borrowerRepository;
    private final KycRecordRepository kycRecordRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public KycService(BorrowerRepository borrowerRepository, KycRecordRepository kycRecordRepository,
                       OtpVerificationRepository otpVerificationRepository) {
        this.borrowerRepository = borrowerRepository;
        this.kycRecordRepository = kycRecordRepository;
        this.otpVerificationRepository = otpVerificationRepository;
    }

    @Transactional
    public OtpInitiateResponse initiate(Long borrowerId, KycInitiateRequest request) {
        requireBorrower(borrowerId);

        KycRecord kycRecord = kycRecordRepository.findByBorrowerId(borrowerId)
                .orElseGet(() -> KycRecord.builder().borrowerId(borrowerId).build());

        applyDocumentNumber(kycRecord, request.documentType(), request.documentNumber());
        kycRecordRepository.save(kycRecord);

        OtpVerification otp = OtpVerification.builder()
                .borrowerId(borrowerId)
                .documentType(request.documentType())
                .otpCode(generateOtpCode())
                .expiresAt(Instant.now().plus(OTP_TTL))
                .build();

        return OtpInitiateResponse.from(otpVerificationRepository.save(otp));
    }

    @Transactional
    public KycResponse verifyOtp(Long borrowerId, OtpVerifyRequest request) {
        Borrower borrower = requireBorrower(borrowerId);

        OtpVerification otp = otpVerificationRepository
                .findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(borrowerId, request.documentType())
                .orElseThrow(() -> new BusinessRuleException(
                        "No OTP has been initiated for " + request.documentType() + " on this borrower"));

        if (otp.isVerified()) {
            throw new BusinessRuleException(request.documentType() + " is already verified for this borrower");
        }
        if (Instant.now().isAfter(otp.getExpiresAt())) {
            throw new BusinessRuleException("OTP has expired - please initiate KYC again");
        }
        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new BusinessRuleException("Maximum OTP attempts exceeded - please initiate KYC again");
        }

        otp.setAttempts(otp.getAttempts() + 1);

        if (!otp.getOtpCode().equals(request.otpCode())) {
            otpVerificationRepository.save(otp);
            throw new BusinessRuleException("Invalid OTP code");
        }

        otp.setVerified(true);
        otpVerificationRepository.save(otp);

        KycRecord kycRecord = kycRecordRepository.findByBorrowerId(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC record not found for borrower " + borrowerId));
        markDocumentVerified(kycRecord, request.documentType());
        kycRecordRepository.save(kycRecord);

        KycLevel newLevel = recomputeKycLevel(kycRecord);
        borrower.setKycLevel(newLevel);
        borrowerRepository.save(borrower);

        return KycResponse.from(kycRecord, newLevel);
    }

    public KycResponse getKyc(Long borrowerId) {
        Borrower borrower = requireBorrower(borrowerId);
        KycRecord kycRecord = kycRecordRepository.findByBorrowerId(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("KYC has not been initiated for borrower " + borrowerId));
        return KycResponse.from(kycRecord, borrower.getKycLevel());
    }

    private void applyDocumentNumber(KycRecord kycRecord, DocumentType documentType, String documentNumber) {
        switch (documentType) {
            case PAN -> {
                if (!documentNumber.equals(kycRecord.getPanNumber()) && kycRecordRepository.existsByPanNumber(documentNumber)) {
                    throw new DuplicateResourceException("PAN " + documentNumber + " is already registered to another borrower");
                }
                kycRecord.setPanNumber(documentNumber);
                kycRecord.setPanVerified(false);
            }
            case AADHAAR -> {
                if (!documentNumber.equals(kycRecord.getAadhaarNumber()) && kycRecordRepository.existsByAadhaarNumber(documentNumber)) {
                    throw new DuplicateResourceException("Aadhaar " + documentNumber + " is already registered to another borrower");
                }
                kycRecord.setAadhaarNumber(documentNumber);
                kycRecord.setAadhaarVerified(false);
            }
        }
    }

    private void markDocumentVerified(KycRecord kycRecord, DocumentType documentType) {
        switch (documentType) {
            case PAN -> kycRecord.setPanVerified(true);
            case AADHAAR -> kycRecord.setAadhaarVerified(true);
        }
    }

    private KycLevel recomputeKycLevel(KycRecord kycRecord) {
        if (kycRecord.isPanVerified() && kycRecord.isAadhaarVerified()) {
            return KycLevel.FULL;
        }
        if (kycRecord.isPanVerified() || kycRecord.isAadhaarVerified()) {
            return KycLevel.BASIC;
        }
        return KycLevel.NONE;
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, OTP_LENGTH);
        int code = secureRandom.nextInt(bound);
        return String.format("%0" + OTP_LENGTH + "d", code);
    }

    private Borrower requireBorrower(Long borrowerId) {
        return borrowerRepository.findById(borrowerId)
                .orElseThrow(() -> new ResourceNotFoundException("Borrower " + borrowerId + " not found"));
    }
}
