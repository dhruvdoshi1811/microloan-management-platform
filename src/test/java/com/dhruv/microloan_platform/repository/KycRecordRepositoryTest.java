package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.entity.KycRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class KycRecordRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;

    @Autowired
    private KycRecordRepository kycRecordRepository;

    private Long persistBorrower(String email) {
        Borrower borrower = Borrower.builder()
                .fullName("Test Borrower")
                .phone("9999999999")
                .email(email)
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000.00"))
                .build();
        return borrowerRepository.save(borrower).getId();
    }

    @Test
    void findsByBorrowerId() {
        Long borrowerId = persistBorrower("erin@example.com");
        kycRecordRepository.save(KycRecord.builder().borrowerId(borrowerId).panNumber("ABCDE1234F").build());

        assertThat(kycRecordRepository.findByBorrowerId(borrowerId)).isPresent();
        assertThat(kycRecordRepository.findByBorrowerId(999L)).isEmpty();
    }

    @Test
    void enforcesUniquePanAndAadhaarAcrossBorrowers() {
        Long borrowerOne = persistBorrower("frank@example.com");
        Long borrowerTwo = persistBorrower("grace@example.com");

        kycRecordRepository.saveAndFlush(
                KycRecord.builder().borrowerId(borrowerOne).panNumber("ABCDE1234F").build());

        assertThat(kycRecordRepository.existsByPanNumber("ABCDE1234F")).isTrue();
        assertThatThrownBy(() -> kycRecordRepository.saveAndFlush(
                KycRecord.builder().borrowerId(borrowerTwo).panNumber("ABCDE1234F").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enforcesOneKycRecordPerBorrower() {
        Long borrowerId = persistBorrower("henry@example.com");
        kycRecordRepository.saveAndFlush(KycRecord.builder().borrowerId(borrowerId).panNumber("ABCDE1234F").build());

        assertThatThrownBy(() -> kycRecordRepository.saveAndFlush(
                KycRecord.builder().borrowerId(borrowerId).aadhaarNumber("123456789012").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
