package mx.edu.unpa.inventory_backend.dtos.guardian.response;

public record GuardianSummary(
        Long id,
        String fullName,
        String employeeNumber,
        String department
) {}
