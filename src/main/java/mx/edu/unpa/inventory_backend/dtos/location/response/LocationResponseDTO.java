package mx.edu.unpa.inventory_backend.dtos.location.response;
/**
 * DTO de salida con los datos públicos de una ubicación.
 */
public record LocationResponseDTO(

        Integer id,
        String  name,
        String  building,
        String  campus,
        String  description,
        Boolean isActive

) {}