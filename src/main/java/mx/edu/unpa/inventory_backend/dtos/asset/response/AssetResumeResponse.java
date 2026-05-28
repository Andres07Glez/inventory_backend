package mx.edu.unpa.inventory_backend.dtos.asset.response;

import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;

public record AssetResumeResponse(
        Long id,
        String inventoryNumber,
        String description,
        String brand,
        String model,
        String categoryName,
        String locationName,
        ConditionStatus conditionStatus,
        LifecycleStatus lifecycleStatus
) {
}
