package com.dhruv.microloan_platform.repository;

import com.dhruv.microloan_platform.entity.Role;
import com.dhruv.microloan_platform.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String email) {
        return User.builder()
                .email(email)
                .passwordHash("hashed-password")
                .role(Role.BORROWER)
                .build();
    }

    @Test
    void savesAndFindsByEmail() {
        userRepository.save(newUser("alice@example.com"));

        assertThat(userRepository.findByEmail("alice@example.com")).isPresent();
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmailReflectsSavedRows() {
        userRepository.save(newUser("bob@example.com"));

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void rejectsDuplicateEmail() {
        userRepository.saveAndFlush(newUser("dupe@example.com"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(newUser("dupe@example.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
