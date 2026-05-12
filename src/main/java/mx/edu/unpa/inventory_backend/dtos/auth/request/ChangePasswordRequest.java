package mx.edu.unpa.inventory_backend.dtos.auth.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
         @NotBlank String currentPassword,
         @NotBlank @Size(min = 8, message = "La nueva contraseña debe tener al menos 8 caracteres")
         String newPassword
 ) {}
