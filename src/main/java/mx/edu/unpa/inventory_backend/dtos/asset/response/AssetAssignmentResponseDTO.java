package mx.edu.unpa.inventory_backend.dtos.asset.response;


import java.time.LocalDateTime;

public record AssetAssignmentResponseDTO(
        Long id,
        String assetInventoryNumber,
        String assetDescription,
        String guardianName, // Asumiendo que Guardian tiene un campo de nombre
        String locationName,
        String notes,
        LocalDateTime assignedAt,
        LocalDateTime returnedAt // Agregamos el campo histórico de tu amigo
) {
}
