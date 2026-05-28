package mx.edu.unpa.inventory_backend.dtos.asset.response;

import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;

/**
 * Proyección ligera para la búsqueda dinámica de bienes durante la creación
 * de incidencias (GET /v1/assets/search).
 *
 * Solo incluye los campos necesarios para identificar y presentar el bien
 * en el selector del formulario de incidencia.
 */
public record AssetSearchResultDTO(
        Long id,
        String inventoryNumber,
        String description,
        String brandName,
        String model,
        String serialNumber,
        ConditionStatus conditionStatus,
        LifecycleStatus lifecycleStatus,
        String categoryName,
        String locationName
) {}