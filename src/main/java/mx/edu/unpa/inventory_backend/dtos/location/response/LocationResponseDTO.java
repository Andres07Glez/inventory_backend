package mx.edu.unpa.inventory_backend.dtos.location.response;

import mx.edu.unpa.inventory_backend.enums.Campus;

/**
 * DTO de salida con los datos públicos de una ubicación.
 */
public record LocationResponseDTO(

        Integer id,
        String  name,
        String  building,
        Campus campus,
        String  description,
        Boolean isActive

) {}