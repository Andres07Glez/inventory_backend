package mx.edu.unpa.inventory_backend.dtos.assetAssigment.request;

import jakarta.validation.constraints.NotNull;

public record AssetAssignmentRequestDTO(
        @NotNull(message = "El ID del bien es obligatorio") Long assetId,
        @NotNull(message = "El ID del resguardante es obligatorio") Long guardianId,
        @NotNull(message = "El ID de la ubicación es obligatorio") Long locationId,
        @NotNull(message = "El ID del usuario que asigna es obligatorio") Long assignedById, // ¡Nuevo!
        String notes
) {
}
