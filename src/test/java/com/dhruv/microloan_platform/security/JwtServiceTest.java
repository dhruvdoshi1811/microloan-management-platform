package com.dhruv.microloan_platform.security;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("unit-test-secret-key-at-least-32-bytes-long-0123456789");
        properties.setExpirationMs(60_000);
        jwtService = new JwtService(properties);
    }

    @Test
    void roundTripsEmailAndRoleClaims() {
        String token = jwtService.generateToken("alice@example.com", "BORROWER");

        assertThat(jwtService.extractEmail(token)).isEqualTo("alice@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("BORROWER");
        assertThat(jwtService.isTokenValid(token, "alice@example.com")).isTrue();
    }

    @Test
    void isTokenValidFalseForWrongEmail() {
        String token = jwtService.generateToken("alice@example.com", "BORROWER");

        assertThat(jwtService.isTokenValid(token, "someone-else@example.com")).isFalse();
    }

    @Test
    void isTokenValidFalseForExpiredToken() {
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret("unit-test-secret-key-at-least-32-bytes-long-0123456789");
        expiredProps.setExpirationMs(-1_000);
        JwtService expiredJwtService = new JwtService(expiredProps);

        String token = expiredJwtService.generateToken("alice@example.com", "BORROWER");

        assertThat(jwtService.isTokenValid(token, "alice@example.com")).isFalse();
    }

    @Test
    void isTokenValidFalseForTamperedToken() {
        String token = jwtService.generateToken("alice@example.com", "BORROWER");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtService.isTokenValid(tampered, "alice@example.com")).isFalse();
    }

    @Test
    void extractEmailThrowsForTamperedSignature() {
        String token = jwtService.generateToken("alice@example.com", "BORROWER");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThatThrownBy(() -> jwtService.extractEmail(tampered))
                .isInstanceOf(SignatureException.class);
    }
}
