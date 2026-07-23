package com.dhruv.microloan_platform.dto.auth;

public record AuthResponse(String token, String tokenType, long expiresInMs) {

    public static AuthResponse bearer(String token, long expiresInMs) {
        return new AuthResponse(token, "Bearer", expiresInMs);
    }
}
