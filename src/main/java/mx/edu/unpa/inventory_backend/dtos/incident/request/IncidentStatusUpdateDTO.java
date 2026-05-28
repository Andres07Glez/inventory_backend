package mx.edu.unpa.inventory_backend.dtos.incident.request;

import jakarta.validation.constraints.NotNull;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.RepairType;

/**
 * Payload para PATCH /v1/incidents/{id}/status.
 *
 * Transiciones permitidas:
 *   OPEN        → IN_PROGRESS  (opcional: repairType)
 *   IN_PROGRESS → RESOLVED     (opcional: resolutionNotes, repairType)
 *
 * Para el cierre definitivo (RESOLVED → CLOSED) usar los endpoints
 * /close o /decommission, que tienen sus propios DTOs.
 */
public record IncidentStatusUpdateDTO(

        @NotNull(message = "El nuevo estado es obligatorio")
        IncidentStatus status

) {}
