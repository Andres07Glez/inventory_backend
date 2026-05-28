package mx.edu.unpa.inventory_backend.dtos.asset.response;

import mx.edu.unpa.inventory_backend.enums.ConditionStatus;

import java.time.LocalDateTime;

public record UpdateConditionResponse(
        Long assetId,
        String inventoryNumber,
        ConditionStatus previousCondition,
        ConditionStatus newCondition,
        LocalDateTime updatedAt
) {}
