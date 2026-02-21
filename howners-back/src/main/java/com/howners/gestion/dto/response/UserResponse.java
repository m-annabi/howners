package com.howners.gestion.dto.response;

import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String phone,
        Role role,
        Boolean enabled,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getRole(),
                user.getEnabled(),
                user.getCreatedAt()
        );
    }
}
