package mx.edu.unpa.inventory_backend.dtos.maintenance.response;

import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respuesta completa de un registro de mantenimiento.
 * Usada en {@code GET /v1/maintenance/{id}}.
 *
 * @param id              Identificador único del registro
 * @param assetId         ID del bien
 * @param inventoryNumber Número de inventario del bien (ej. INV-2026-00001)
 * @param assetDescription Descripción del bien
 * @param incidentId      ID de la incidencia vinculada (puede ser null)
 * @param maintenanceType Tipo de mantenimiento
 * @param description     Trabajo realizado
 * @param performedBy     Técnico o empresa (puede ser null)
 * @param performedDate   Fecha real del servicio
 * @param cost            Costo (puede ser null)
 * @param conditionBefore Condición antes del servicio (puede ser null)
 * @param conditionAfter  Condición después del servicio (puede ser null)
 * @param createdAt       Timestamp de registro en el sistema
 * @param createdByName   Nombre completo del usuario que registró
 */
public record MaintenanceResponse(
        Long id,
        Long assetId,
        String inventoryNumber,
        String assetDescription,
        Long incidentId,
        MaintenanceType maintenanceType,
        String description,
        String performedBy,
        LocalDate performedDate,
        BigDecimal cost,
        ConditionStatus conditionBefore,
        ConditionStatus conditionAfter,
        LocalDateTime createdAt,
        String createdByName
) {}
