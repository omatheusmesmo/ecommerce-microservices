package com.ecommerce.dto;

import com.ecommerce.entity.Role;
import com.ecommerce.entity.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.id,
                user.email,
                user.fullName,
                user.role,
                user.active,
                user.createdAt
        );
    }
}