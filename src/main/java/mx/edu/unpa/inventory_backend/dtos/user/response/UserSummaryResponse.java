package mx.edu.unpa.inventory_backend.dtos.user.response;

import mx.edu.unpa.inventory_backend.enums.UserRole;

import java.time.LocalDateTime;

public record UserSummaryResponse(
        Long id,
        String username,
        String fullName,
        String email,
        String employeeNumber,
        UserRole role,
        boolean isActive,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {}
