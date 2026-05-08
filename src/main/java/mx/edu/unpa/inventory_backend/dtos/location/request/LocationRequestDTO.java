package mx.edu.unpa.inventory_backend.dtos.location.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LocationRequestDTO(

        @NotBlank(message = "El nombre de la ubicación es obligatorio")
        @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
        String name,

        @Size(max = 100, message = "El edificio no puede exceder 100 caracteres")
        String building,

        @Size(max = 100, message = "El campus no puede exceder 100 caracteres")
        String campus,

        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String description

) {}