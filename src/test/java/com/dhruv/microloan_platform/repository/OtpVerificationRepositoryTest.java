package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.DocumentType;
import com.dhruv.microloan_platform.entity.OtpVerification;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OtpVerificationRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private OtpVerificationRepository otpVerificationRepository;

    private Long persistBorrower() {
        Borrower borrower = Borrower.builder()
                .fullName("Test Borrower")
                .phone("9999999999")
                .email("otp-repo-test@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000.00"))
                .build();
        return borrowerRepository.save(borrower).getId();
    }

    private OtpVerification newOtp(Long borrowerId, String code) {
        return OtpVerification.builder()
                .borrowerId(borrowerId)
                .documentType(DocumentType.PAN)
                .otpCode(code)
                .expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES))
                .build();
    }

    @Test
    void findsMostRecentOtpForBorrowerAndDocumentType() {
        Long borrowerId = persistBorrower();
        otpVerificationRepository.save(newOtp(borrowerId, "111111"));
        OtpVerification latest = otpVerificationRepository.save(newOtp(borrowerId, "222222"));

        OtpVerification found = otpVerificationRepository
                .findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(borrowerId, DocumentType.PAN)
                .orElseThrow();

        assertThat(found.getId()).isEqualTo(latest.getId());
        assertThat(found.getOtpCode()).isEqualTo("222222");
    }

    @Test
    void returnsEmptyWhenNoOtpIssuedForDocumentType() {
        Long borrowerId = persistBorrower();
        otpVerificationRepository.save(newOtp(borrowerId, "111111"));

        assertThat(otpVerificationRepository
                .findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(borrowerId, DocumentType.AADHAAR))
                .isEmpty();
    }
}
