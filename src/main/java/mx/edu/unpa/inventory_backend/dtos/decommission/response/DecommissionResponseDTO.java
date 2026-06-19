package mx.edu.unpa.inventory_backend.dtos.decommission.response;

import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respuesta completa para GET /v1/decommissions/{id}
 * y como resultado de operaciones POST/PATCH.
 *
 * decommissionDocumentUrl es null si no se adjuntó documento.
 * incidentId / incidentFolio son null si la baja no proviene de una incidencia.
 */
public record DecommissionResponseDTO(

        Long id,

        // ── Bien dado de baja ───────────────────────────────────────────────
        Long assetId,
        String assetInventoryNumber,
        String assetDescription,

        // ── Incidencia de origen (OPCIONAL) ────────────────────────────────
        Long incidentId,
        String incidentFolio,

        // ── Datos de la baja ────────────────────────────────────────────────
        String justification,
        String decommissionDocumentUrl,
        LocalDate decommissionDate,
        DecommissionStatus status,

        // ── Auditoría ───────────────────────────────────────────────────────
        LocalDateTime createdAt,
        String createdByName,
        LocalDateTime confirmedAt,
        String confirmedByName
) {}