package mx.edu.unpa.inventory_backend.dtos.user.request;

import jakarta.validation.constraints.NotNull;
import mx.edu.unpa.inventory_backend.enums.UserRole;

public record UpdateUserRoleRequest(
        @NotNull(message = "El rol es obligatorio")
        UserRole role
) {}
