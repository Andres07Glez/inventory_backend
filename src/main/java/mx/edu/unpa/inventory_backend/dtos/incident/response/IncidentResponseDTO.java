package mx.edu.unpa.inventory_backend.dtos.incident.response;

import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.enums.ClosureType;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.RepairType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta completa para GET /v1/incidents/{id}.
 *
 * REFACTORIZACIÓN SP-16:
 *   Eliminados: decommissionJustification, decommissionDocumentUrl.
 *   La información de baja ahora vive en DecommissionResponseDTO.
 *
 *   Si la incidencia derivó en una baja, el frontend puede consultarla con:
 *     GET /v1/assets/{assetId}/decommission
 */
public record IncidentResponseDTO(

        Long id,

        /** Folio legible. Ej.: INC-2026-00003. Se calcula en el servicio. */
        String folio,

        // ── Bien vinculado ──────────────────────────────────────────────────
        Long assetId,
        String assetInventoryNumber,
        String assetDescription,

        // ── Datos de la incidencia ──────────────────────────────────────────
        String description,
        RepairType repairType,
        IncidentStatus status,
        ConditionStatus conditionAtIncident,

        /** Fecha en que ocurrió la incidencia. Puede diferir de createdAt. */
        LocalDate incidentDate,

        // ── Resolución ─────────────────────────────────────────────────────
        String resolutionNotes,
        LocalDateTime resolvedAt,
        String resolvedByName,
        //ClosureType closureType,


        // ── Auditoría ───────────────────────────────────────────────────────
        LocalDateTime createdAt,
        String createdByName,

        List<IncidentImageResponseDTO> images
) {}


