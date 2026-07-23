package com.dhruv.microloan_platform.dto.kyc;

import com.dhruv.microloan_platform.entity.KycLevel;
import com.dhruv.microloan_platform.entity.KycRecord;

public record KycResponse(
        Long borrowerId,
        String panNumber,
        String aadhaarNumber,
        boolean panVerified,
        boolean aadhaarVerified,
        KycLevel kycLevel
) {

    public static KycResponse from(KycRecord kycRecord, KycLevel kycLevel) {
        return new KycResponse(
                kycRecord.getBorrowerId(),
                kycRecord.getPanNumber(),
                kycRecord.getAadhaarNumber(),
                kycRecord.isPanVerified(),
                kycRecord.isAadhaarVerified(),
                kycLevel);
    }
}
