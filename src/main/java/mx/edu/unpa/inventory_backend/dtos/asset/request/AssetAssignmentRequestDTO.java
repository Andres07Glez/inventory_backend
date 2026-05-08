package mx.edu.unpa.inventory_backend.dtos.asset.request;

import jakarta.validation.constraints.NotNull;

public record AssetAssignmentRequestDTO(
        @NotNull(message = "El ID del bien es obligatorio") Long assetId,
        @NotNull(message = "El ID del resguardante es obligatorio") Long guardianId,
        @NotNull(message = "El ID del usuario es obligatorio") Long assignedBy,
        String notes
) {
}
