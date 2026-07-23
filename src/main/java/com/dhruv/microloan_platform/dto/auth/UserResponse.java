package com.dhruv.microloan_platform.dto.auth;

import com.dhruv.microloan_platform.entity.Role;
import com.dhruv.microloan_platform.entity.User;

import java.time.Instant;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
