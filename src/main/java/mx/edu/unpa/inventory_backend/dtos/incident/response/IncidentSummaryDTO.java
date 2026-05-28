package mx.edu.unpa.inventory_backend.dtos.incident.response;

import mx.edu.unpa.inventory_backend.enums.ClosureType;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.RepairType;

import java.time.LocalDateTime;

/**
 * Vista resumida para la pestaña de incidencias en el detalle del bien
 * y para el listado global paginado.
 */
public record IncidentSummaryDTO(
        Long id,
        String folio,
        String description,
        IncidentStatus status,
        ConditionStatus conditionAtIncident,
        RepairType repairType,
        ClosureType closureType,
        LocalDateTime createdAt,
        String createdByName
) {}
