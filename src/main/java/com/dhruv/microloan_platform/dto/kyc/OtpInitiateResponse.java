package com.dhruv.microloan_platform.dto.kyc;

import com.dhruv.microloan_platform.entity.DocumentType;
import com.dhruv.microloan_platform.entity.OtpVerification;

import java.time.Instant;

/**
 * Returned by kyc/initiate. There's no real SMS/UIDAI gateway in this project, so the
 * generated code is handed back directly here for dev/demo testing - not something a real
 * system would ever do.
 */
public record OtpInitiateResponse(Long borrowerId, DocumentType documentType, String otpCode, Instant expiresAt) {

    public static OtpInitiateResponse from(OtpVerification otp) {
        return new OtpInitiateResponse(otp.getBorrowerId(), otp.getDocumentType(), otp.getOtpCode(), otp.getExpiresAt());
    }
}
