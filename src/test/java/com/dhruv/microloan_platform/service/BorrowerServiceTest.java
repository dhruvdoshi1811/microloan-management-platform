package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.borrower.BorrowerRequest;
import com.dhruv.microloan_platform.dto.borrower.BorrowerResponse;
import com.dhruv.microloan_platform.entity.Borrower;
import com.dhruv.microloan_platform.exception.DuplicateResourceException;
import com.dhruv.microloan_platform.exception.ResourceNotFoundException;
import com.dhruv.microloan_platform.repository.BorrowerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowerServiceTest {

    @Mock
    private BorrowerRepository borrowerRepository;

    @InjectMocks
    private BorrowerService borrowerService;

    private BorrowerRequest request(String email) {
        return new BorrowerRequest("Alice Borrower", "9999999999", email,
                LocalDate.of(1995, 1, 1), new BigDecimal("50000.00"));
    }

    private Borrower existing(Long id, String email) {
        return Borrower.builder()
                .id(id)
                .fullName("Alice Borrower")
                .phone("9999999999")
                .email(email)
                .dob(LocalDate.of(1995, 1, 1))
                .monthlyIncome(new BigDecimal("50000.00"))
                .build();
    }

    @Test
    void createSavesNewBorrower() {
        when(borrowerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> {
            Borrower saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        BorrowerResponse response = borrowerService.create(request("alice@example.com"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.email()).isEqualTo("alice@example.com");
    }

    @Test
    void createRejectsDuplicateEmail() {
        when(borrowerRepository.existsByEmail("dupe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> borrowerService.create(request("dupe@example.com")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(borrowerRepository, never()).save(any());
    }

    @Test
    void getThrowsWhenBorrowerMissing() {
        when(borrowerRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> borrowerService.get(42L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateChangesFieldsWhenEmailUnchanged() {
        Borrower existing = existing(1L, "alice@example.com");
        when(borrowerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(borrowerRepository.save(any(Borrower.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BorrowerRequest update = new BorrowerRequest("Alice Updated", "8888888888", "alice@example.com",
                LocalDate.of(1995, 1, 1), new BigDecimal("75000.00"));

        BorrowerResponse response = borrowerService.update(1L, update);

        assertThat(response.fullName()).isEqualTo("Alice Updated");
        assertThat(response.phone()).isEqualTo("8888888888");
        assertThat(response.monthlyIncome()).isEqualByComparingTo("75000.00");
    }

    @Test
    void updateRejectsEmailChangeToAnExistingBorrowersEmail() {
        Borrower existing = existing(1L, "alice@example.com");
        when(borrowerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(borrowerRepository.existsByEmail("taken@example.com")).thenReturn(true);

        BorrowerRequest update = request("taken@example.com");

        assertThatThrownBy(() -> borrowerService.update(1L, update))
                .isInstanceOf(DuplicateResourceException.class);

        verify(borrowerRepository, never()).save(any());
    }
}
