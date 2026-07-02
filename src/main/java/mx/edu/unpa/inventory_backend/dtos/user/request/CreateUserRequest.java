package mx.edu.unpa.inventory_backend.dtos.user.request;

import jakarta.validation.constraints.*;
import mx.edu.unpa.inventory_backend.enums.UserRole;

public record CreateUserRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50)
        String username,

        @NotNull(message = "El rol es obligatorio")
        UserRole role,

        @NotNull(message = "El ID del guardian es obligatorio")// Nuevo: ID del guardian a vincular (reemplaza fullName, email, employeeNumber)
        Long guardianId
) {}
