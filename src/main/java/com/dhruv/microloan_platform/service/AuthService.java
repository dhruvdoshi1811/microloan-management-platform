package com.dhruv.microloan_platform.service;

import com.dhruv.microloan_platform.dto.auth.AuthResponse;
import com.dhruv.microloan_platform.dto.auth.LoginRequest;
import com.dhruv.microloan_platform.dto.auth.RegisterRequest;
import com.dhruv.microloan_platform.dto.auth.UserResponse;
import com.dhruv.microloan_platform.entity.User;
import com.dhruv.microloan_platform.exception.DuplicateResourceException;
import com.dhruv.microloan_platform.repository.UserRepository;
import com.dhruv.microloan_platform.security.JwtProperties;
import com.dhruv.microloan_platform.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Register creates the account only - it deliberately does not issue a token. Login is a
 * separate call that goes through Spring Security's {@link AuthenticationManager}, which is
 * what actually checks the password against the BCrypt hash.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager, JwtService jwtService,
                        JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("A user with email " + request.email() + " already exists");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalStateException(
                        "User authenticated successfully but vanished from the database: " + request.email()));

        String token = jwtService.generateToken(user.getEmail(), user.getRole().name());
        return AuthResponse.bearer(token, jwtProperties.getExpirationMs());
    }

    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated principal vanished from the database: " + email));
        return UserResponse.from(user);
    }
}
