package mx.edu.unpa.inventory_backend.dtos.incident.request;

import jakarta.validation.constraints.NotBlank;
import mx.edu.unpa.inventory_backend.enums.RepairType;

/**
 * Payload para POST /v1/incidents/{id}/close (cierre STANDARD).
 * Accesible por ADMIN y OPERADOR.
 * Para baja definitiva usar POST /v1/incidents/{id}/decommission.
 */
public record IncidentCloseRequestDTO(

        @NotBlank(message = "Las notas de resolución son obligatorias para cerrar la incidencia")
        String resolutionNotes,

        /** Obligatorio si aún no se había definido. */
        RepairType repairType
) {}
