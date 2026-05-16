package mx.edu.unpa.inventory_backend.dtos.asset.response;

import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianSummary;
import mx.edu.unpa.inventory_backend.dtos.image.response.AssetImageResponseDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AssetDetailResponse(
        Long id,
        String inventoryNumber,
        String barcode,
        String description,
        String brand,
        String model,
        String serialNumber,
        String categoryName,
        String locationName,
        String building,
        String campus,
        ConditionStatus conditionStatus,
        LifecycleStatus lifecycleStatus,
        LocalDate entryDate,
        LocalDateTime updatedAt,
        GuardianSummary guardian,          // null si no hay asignación activa
        List<AssetImageResponseDTO> images
) {}
