package mx.edu.unpa.inventory_backend.dtos.guardian.response;


public record GuardianResponseDTO(
        Long    id,
        String  employeeNumber,
        String  fullName,
        String  email,
        String  phone,
        String  department,
        Integer locationId,
        String  locationName,
        Boolean isActive
) {}


