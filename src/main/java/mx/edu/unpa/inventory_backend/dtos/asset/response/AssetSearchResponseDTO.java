package mx.edu.unpa.inventory_backend.dtos.asset.response;

import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;

public record AssetSearchResponseDTO(
        Long id,
        String inventoryNumber,
        String description,
        String brand,
        String model,
        String categoryName,
        ConditionStatus conditionStatus,
        LifecycleStatus lifecycleStatus,
        String locationName,
        String currentGuardianName // Aquí mostraremos al resguardante actual
) {
}
