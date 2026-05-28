package mx.edu.unpa.inventory_backend.dtos.auth.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El número de empleado es obligatorio")
        String employeeNumber,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}
