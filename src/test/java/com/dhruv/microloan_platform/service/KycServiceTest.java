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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KycServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;
    @Mock
    private KycRecordRepository kycRecordRepository;
    @Mock
    private OtpVerificationRepository otpVerificationRepository;

    @InjectMocks
    private KycService kycService;

    private static final Long BORROWER_ID = 1L;

    private Borrower borrower() {
        return Borrower.builder()
                .id(BORROWER_ID)
                .fullName("Alice Borrower")
                .phone("9999999999")
                .email("alice@example.com")
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000.00"))
                .kycLevel(KycLevel.NONE)
                .build();
    }

    private void stubBorrowerFound(Borrower borrower) {
        when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.of(borrower));
    }

    // ---- initiate ----

    @Test
    void initiateThrowsWhenBorrowerMissing() {
        when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycService.initiate(BORROWER_ID, new KycInitiateRequest(DocumentType.PAN, "ABCDE1234F")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void initiateCreatesKycRecordAndOtpWhenNoneExists() {
        stubBorrowerFound(borrower());
        when(kycRecordRepository.findByBorrowerId(BORROWER_ID)).thenReturn(Optional.empty());
        when(kycRecordRepository.existsByPanNumber("ABCDE1234F")).thenReturn(false);
        when(kycRecordRepository.save(any(KycRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        OtpInitiateResponse response = kycService.initiate(BORROWER_ID, new KycInitiateRequest(DocumentType.PAN, "ABCDE1234F"));

        assertThat(response.borrowerId()).isEqualTo(BORROWER_ID);
        assertThat(response.documentType()).isEqualTo(DocumentType.PAN);
        assertThat(response.otpCode()).hasSize(6);
        assertThat(response.otpCode()).containsOnlyDigits();

        ArgumentCaptor<KycRecord> recordCaptor = ArgumentCaptor.forClass(KycRecord.class);
        verify(kycRecordRepository).save(recordCaptor.capture());
        assertThat(recordCaptor.getValue().getPanNumber()).isEqualTo("ABCDE1234F");
        assertThat(recordCaptor.getValue().isPanVerified()).isFalse();
    }

    @Test
    void initiateRejectsPanAlreadyRegisteredToAnotherBorrower() {
        stubBorrowerFound(borrower());
        when(kycRecordRepository.findByBorrowerId(BORROWER_ID)).thenReturn(Optional.empty());
        when(kycRecordRepository.existsByPanNumber("ABCDE1234F")).thenReturn(true);

        assertThatThrownBy(() -> kycService.initiate(BORROWER_ID, new KycInitiateRequest(DocumentType.PAN, "ABCDE1234F")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(otpVerificationRepository, never()).save(any());
    }

    @Test
    void initiateAllowsReinitiatingWithTheSamePanNumberAlreadyOnFile() {
        stubBorrowerFound(borrower());
        KycRecord existingRecord = KycRecord.builder().borrowerId(BORROWER_ID).panNumber("ABCDE1234F").panVerified(true).build();
        when(kycRecordRepository.findByBorrowerId(BORROWER_ID)).thenReturn(Optional.of(existingRecord));
        when(kycRecordRepository.save(any(KycRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        kycService.initiate(BORROWER_ID, new KycInitiateRequest(DocumentType.PAN, "ABCDE1234F"));

        // Same number as already on file -> no uniqueness lookup needed, and re-initiating resets verification.
        verify(kycRecordRepository, never()).existsByPanNumber(any());
        assertThat(existingRecord.isPanVerified()).isFalse();
    }

    // ---- verifyOtp ----

    @Test
    void verifyOtpThrowsWhenNoOtpInitiated() {
        stubBorrowerFound(borrower());
        when(otpVerificationRepository.findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(BORROWER_ID, DocumentType.PAN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycService.verifyOtp(BORROWER_ID, new OtpVerifyRequest(DocumentType.PAN, "123456")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void verifyOtpThrowsWhenAlreadyVerified() {
        stubBorrowerFound(borrower());
        OtpVerification otp = otp("123456", Instant.now().plus(5, ChronoUnit.MINUTES), true, 0);
        when(otpVerificationRepository.findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(BORROWER_ID, DocumentType.PAN))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> kycService.verifyOtp(BORROWER_ID, new OtpVerifyRequest(DocumentType.PAN, "123456")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void verifyOtpThrowsWhenExpired() {
        stubBorrowerFound(borrower());
        OtpVerification otp = otp("123456", Instant.now().minus(1, ChronoUnit.MINUTES), false, 0);
        when(otpVerificationRepository.findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(BORROWER_ID, DocumentType.PAN))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> kycService.verifyOtp(BORROWER_ID, new OtpVerifyRequest(DocumentType.PAN, "123456")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void verifyOtpThrowsWhenMaxAttemptsExceeded() {
        stubBorrowerFound(borrower());
        OtpVerification otp = otp("123456", Instant.now().plus(5, ChronoUnit.MINUTES), false, 5);
        when(otpVerificationRepository.findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(BORROWER_ID, DocumentType.PAN))
                .thenReturn(Optional.of(otp));

        assertThatThrownBy(() -> kycService.verifyOtp(BORROWER_ID, new OtpVerifyRequest(DocumentType.PAN, "123456")))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void verifyOtpWrongCodeIncrementsAttemptsAndThrows() {
        stubBorrowerFound(borrower());
        OtpVerification otp = otp("123456", Instant.now().plus(5, ChronoUnit.MINUTES), false, 0);
        when(otpVerificationRepository.findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(BORROWER_ID, DocumentType.PAN))
                .thenReturn(Optional.of(otp));
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> kycService.verifyOtp(BORROWER_ID, new OtpVerifyRequest(DocumentType.PAN, "000000")))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(otp.getAttempts()).isEqualTo(1);
        assertThat(otp.isVerified()).isFalse();
    }

    @Test
    void verifyOtpSuccessSetsBasicLevelWhenOnlyOneDocumentVerified() {
        Borrower borrower = borrower();
        stubBorrowerFound(borrower);
        OtpVerification otp = otp("123456", Instant.now().plus(5, ChronoUnit.MINUTES), false, 0);
        when(otpVerificationRepository.findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(BORROWER_ID, DocumentType.PAN))
                .thenReturn(Optional.of(otp));
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        KycRecord kycRecord = KycRecord.builder().borrowerId(BORROWER_ID).panNumber("ABCDE1234F").build();
        when(kycRecordRepository.findByBorrowerId(BORROWER_ID)).thenReturn(Optional.of(kycRecord));
        when(kycRecordRepository.save(any(KycRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));

        KycResponse response = kycService.verifyOtp(BORROWER_ID, new OtpVerifyRequest(DocumentType.PAN, "123456"));

        assertThat(response.panVerified()).isTrue();
        assertThat(response.kycLevel()).isEqualTo(KycLevel.BASIC);
        assertThat(borrower.getKycLevel()).isEqualTo(KycLevel.BASIC);
        assertThat(otp.isVerified()).isTrue();
    }

    @Test
    void verifyOtpSuccessSetsFullLevelWhenBothDocumentsVerified() {
        Borrower borrower = borrower();
        stubBorrowerFound(borrower);
        OtpVerification otp = otp(DocumentType.AADHAAR, "654321", Instant.now().plus(5, ChronoUnit.MINUTES), false, 0);
        when(otpVerificationRepository.findTopByBorrowerIdAndDocumentTypeOrderByIdDesc(BORROWER_ID, DocumentType.AADHAAR))
                .thenReturn(Optional.of(otp));
        when(otpVerificationRepository.save(any(OtpVerification.class))).thenAnswer(inv -> inv.getArgument(0));

        // PAN already verified from an earlier call; this call verifies Aadhaar too.
        KycRecord kycRecord = KycRecord.builder()
                .borrowerId(BORROWER_ID)
                .panNumber("ABCDE1234F").panVerified(true)
                .aadhaarNumber("123456789012")
                .build();
        when(kycRecordRepository.findByBorrowerId(BORROWER_ID)).thenReturn(Optional.of(kycRecord));
        when(kycRecordRepository.save(any(KycRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(inv -> inv.getArgument(0));

        KycResponse response = kycService.verifyOtp(BORROWER_ID, new OtpVerifyRequest(DocumentType.AADHAAR, "654321"));

        assertThat(response.aadhaarVerified()).isTrue();
        assertThat(response.kycLevel()).isEqualTo(KycLevel.FULL);
        assertThat(borrower.getKycLevel()).isEqualTo(KycLevel.FULL);
    }

    // ---- getKyc ----

    @Test
    void getKycThrowsWhenBorrowerMissing() {
        when(borrowerRepository.findById(BORROWER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycService.getKyc(BORROWER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getKycThrowsWhenNotYetInitiated() {
        stubBorrowerFound(borrower());
        when(kycRecordRepository.findByBorrowerId(BORROWER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> kycService.getKyc(BORROWER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getKycReturnsCurrentState() {
        Borrower borrower = borrower();
        borrower.setKycLevel(KycLevel.BASIC);
        stubBorrowerFound(borrower);
        KycRecord kycRecord = KycRecord.builder().borrowerId(BORROWER_ID).panNumber("ABCDE1234F").panVerified(true).build();
        when(kycRecordRepository.findByBorrowerId(BORROWER_ID)).thenReturn(Optional.of(kycRecord));

        KycResponse response = kycService.getKyc(BORROWER_ID);

        assertThat(response.kycLevel()).isEqualTo(KycLevel.BASIC);
        assertThat(response.panVerified()).isTrue();
    }

    private OtpVerification otp(String code, Instant expiresAt, boolean verified, int attempts) {
        return otp(DocumentType.PAN, code, expiresAt, verified, attempts);
    }

    private OtpVerification otp(DocumentType documentType, String code, Instant expiresAt, boolean verified, int attempts) {
        return OtpVerification.builder()
                .borrowerId(BORROWER_ID)
                .documentType(documentType)
                .otpCode(code)
                .expiresAt(expiresAt)
                .verified(verified)
                .attempts(attempts)
                .build();
    }
}
