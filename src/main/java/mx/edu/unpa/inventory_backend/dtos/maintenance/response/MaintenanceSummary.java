package mx.edu.unpa.inventory_backend.dtos.maintenance.response;

import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Vista resumida de un registro de mantenimiento para listas y tabs.
 * Excluye {@code description} completa y datos de auditoría verbosos.
 * Usada en {@code GET /v1/maintenance} y {@code GET /v1/assets/{assetId}/maintenance}.
 *
 * @param id              Identificador único
 * @param assetId         ID del bien
 * @param inventoryNumber Número de inventario del bien
 * @param incidentId      ID de la incidencia vinculada (puede ser null)
 * @param maintenanceType Tipo de mantenimiento
 * @param performedBy     Técnico o empresa (puede ser null)
 * @param performedDate   Fecha real del servicio
 * @param cost            Costo (puede ser null)
 * @param conditionBefore Condición antes (puede ser null)
 * @param conditionAfter  Condición después (puede ser null)
 * @param createdByName   Nombre del usuario que registró
 */
public record MaintenanceSummary(
        Long id,
        Long assetId,
        String inventoryNumber,
        Long incidentId,
        MaintenanceType maintenanceType,
        String performedBy,
        LocalDate performedDate,
        BigDecimal cost,
        ConditionStatus conditionBefore,
        ConditionStatus conditionAfter,
        String createdByName
) {}
