package mx.edu.unpa.inventory_backend.dtos.category.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String description,

        /** ID de la categoría padre. Null = categoría raíz. */
        Integer parentId

) {}
