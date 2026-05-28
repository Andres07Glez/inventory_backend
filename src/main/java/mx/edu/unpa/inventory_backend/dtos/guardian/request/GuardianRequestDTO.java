package mx.edu.unpa.inventory_backend.dtos.guardian.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GuardianRequestDTO(

        @NotBlank(message = "El número de empleado es obligatorio")
        @Size(max = 30, message = "El número de empleado no puede exceder 30 caracteres")
        String employeeNumber,

        @NotBlank(message = "El nombre completo es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String fullName,

        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String email,

        @Size(max = 25, message = "El teléfono no puede exceder 25 caracteres")
        String phone,

        @Size(max = 150, message = "El departamento no puede exceder 150 caracteres")
        String department,

        Integer locationId

) {}