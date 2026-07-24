package com.dhruv.microloan_platform.dto.kyc;

import com.dhruv.microloan_platform.entity.DocumentType;
import com.dhruv.microloan_platform.entity.OtpVerification;

import java.time.Instant;

public record OtpInitiateResponse(Long borrowerId, DocumentType documentType, String otpCode, Instant expiresAt) {

    public static OtpInitiateResponse from(OtpVerification otp) {
        return new OtpInitiateResponse(otp.getBorrowerId(), otp.getDocumentType(), otp.getOtpCode(), otp.getExpiresAt());
    }
}
