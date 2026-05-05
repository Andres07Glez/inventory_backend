package mx.edu.unpa.inventory_backend.dtos.supplier.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupplierRequestDTO(

        @NotBlank(message = "El nombre del proveedor es obligatorio")
        @Size(max = 200, message = "El nombre no puede exceder 200 caracteres")
        String name,

        @Size(max = 150, message = "El nombre de contacto no puede exceder 150 caracteres")
        String contactName,

        @Email(message = "El correo electrónico no tiene un formato válido")
        @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
        String email,

        @Size(max = 25, message = "El teléfono no puede exceder 25 caracteres")
        String phone,

        @Size(max = 300, message = "La dirección no puede exceder 300 caracteres")
        String address,

        String notes

) {}
