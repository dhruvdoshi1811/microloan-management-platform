package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Borrower;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class BorrowerRepositoryTest {

    @Autowired
    private BorrowerRepository borrowerRepository;

    private Borrower newBorrower(String email) {
        return Borrower.builder()
                .fullName("Test Borrower")
                .phone("9999999999")
                .email(email)
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000.00"))
                .build();
    }

    @Test
    void savesWithDefaultKycLevelAndActiveFlag() {
        Borrower saved = borrowerRepository.save(newBorrower("carol@example.com"));

        Borrower found = borrowerRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getKycLevel().name()).isEqualTo("NONE");
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void existsByEmailReflectsSavedRows() {
        borrowerRepository.save(newBorrower("dave@example.com"));

        assertThat(borrowerRepository.existsByEmail("dave@example.com")).isTrue();
        assertThat(borrowerRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void rejectsDuplicateEmail() {
        borrowerRepository.saveAndFlush(newBorrower("dupe@example.com"));

        assertThatThrownBy(() -> borrowerRepository.saveAndFlush(newBorrower("dupe@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
