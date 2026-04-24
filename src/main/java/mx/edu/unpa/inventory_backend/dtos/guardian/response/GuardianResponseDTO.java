package mx.edu.unpa.inventory_backend.dtos.guardian.response;

/**
 * DTO de salida con los datos públicos de un resguardante.
 * Se retorna en todos los endpoints GET y POST del módulo.
 */
public record GuardianResponseDTO(

        Long   id,
        String employeeNumber,
        String fullName,
        String email,
        String phone,
        String department,
        Boolean isActive

) {}

