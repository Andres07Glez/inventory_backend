package mx.edu.unpa.inventory_backend.dtos.incident.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.RepairType;

import java.time.LocalDate;

/**
 * Payload para abrir una nueva incidencia.
 * El bien se selecciona previamente con GET /v1/assets/search.
 */
public record IncidentRequestDTO(

        @NotNull(message = "El ID del bien es obligatorio")
        @Positive(message = "El ID del bien debe ser positivo")
        Long assetId,

        @NotBlank(message = "La descripción de la incidencia es obligatoria")
        String description,

        /**
         * Fecha en que ocurrió la incidencia.
         * Null = hoy. No puede ser futura.
         */
        @PastOrPresent(message = "La fecha de la incidencia no puede ser futura")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate incidentDate,

        @NotNull(message = "La condición al momento del reporte es obligatoria")
        ConditionStatus conditionAtIncident,

        /** Opcional al abrir; puede actualizarse al resolver. */
        RepairType repairType
) {}
