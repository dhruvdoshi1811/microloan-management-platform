package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.auth.AuthResponse;
import com.dhruv.microloan_platform.dto.auth.LoginRequest;
import com.dhruv.microloan_platform.dto.auth.RegisterRequest;
import com.dhruv.microloan_platform.dto.auth.UserResponse;
import com.dhruv.microloan_platform.entity.Role;
import com.dhruv.microloan_platform.entity.User;
import com.dhruv.microloan_platform.exception.DuplicateResourceException;
import com.dhruv.microloan_platform.repository.UserRepository;
import com.dhruv.microloan_platform.security.JwtProperties;
import com.dhruv.microloan_platform.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private AuthService authService() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("does-not-matter-for-this-test");
        jwtProperties.setExpirationMs(3_600_000);
        return new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, jwtProperties);
    }

    @Test
    void registerHashesPasswordAndSavesUser() {
        RegisterRequest request = new RegisterRequest("alice@example.com", "plain-password", Role.BORROWER);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(1L);
            saved.setCreatedAt(Instant.now());
            return saved;
        });

        UserResponse response = authService().register(request);

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(Role.BORROWER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed-password");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("dupe@example.com", "plain-password", Role.BORROWER);
        when(userRepository.existsByEmail("dupe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService().register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loginAuthenticatesAndIssuesToken() {
        LoginRequest request = new LoginRequest("alice@example.com", "plain-password");
        User user = User.builder().id(1L).email("alice@example.com").role(Role.BORROWER).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("alice@example.com", "BORROWER")).thenReturn("signed-jwt");

        AuthResponse response = authService().login(request);

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.tokenType()).isEqualTo("Bearer");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("alice@example.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("plain-password");
    }

    @Test
    void loginPropagatesBadCredentials() {
        LoginRequest request = new LoginRequest("alice@example.com", "wrong-password");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authService().login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void meReturnsCurrentUser() {
        User user = User.builder().id(1L).email("alice@example.com").role(Role.ADMIN).build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserResponse response = authService().me("alice@example.com");

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.role()).isEqualTo(Role.ADMIN);
    }
}
