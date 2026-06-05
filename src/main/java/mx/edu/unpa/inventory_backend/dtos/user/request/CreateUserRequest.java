package mx.edu.unpa.inventory_backend.dtos.user.request;

import jakarta.validation.constraints.*;
import mx.edu.unpa.inventory_backend.enums.UserRole;

public record CreateUserRequest(
        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 50)
        String username,

        //@NotBlank(message = "El nombre completo es obligatorio")
        // Opcional si se proporciona guardianId
        /*@Size(max = 150)
        String fullName,

        //@NotBlank(message = "El correo es obligatorio")
        // Opcional si se proporciona guardianId
        @Email(message = "Formato de correo inválido")
        String email,

        // Opcional si se proporciona guardianId
        @Pattern(regexp = "^EMP-\\d{3,6}$", message = "Formato esperado: EMP-000")
        String employeeNumber,*/

        @NotNull(message = "El rol es obligatorio")
        UserRole role,

        @NotNull(message = "El ID del guardian es obligatorio")// Nuevo: ID del guardian a vincular (reemplaza fullName, email, employeeNumber)
        Long guardianId
) {}
