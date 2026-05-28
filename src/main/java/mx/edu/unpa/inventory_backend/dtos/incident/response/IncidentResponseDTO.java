package mx.edu.unpa.inventory_backend.dtos.incident.response;

import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.enums.ClosureType;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.RepairType;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta completa para GET /v1/incidents/{id}.
 * Incluye folio calculado, datos del bien, usuario y lista de imágenes de evidencia.
 */
public record IncidentResponseDTO(

        Long id,

        /** Folio legible. Ej.: INC-2026-00003. No se almacena; se calcula en el servicio. */
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

        // ── Resolución ─────────────────────────────────────────────────────
        String resolutionNotes,
        LocalDateTime resolvedAt,
        String resolvedByName,

        // ── Cierre / Baja ───────────────────────────────────────────────────
        ClosureType closureType,
        String decommissionJustification,

        /**
         * URL pública del acta PDF de baja, construida via StorageService.
         * Null si no aplica o aún no se ha subido.
         */
        String decommissionDocumentUrl,

        // ── Auditoría ───────────────────────────────────────────────────────
        LocalDateTime createdAt,
        String createdByName,

        List<IncidentImageResponseDTO> images
) {}


