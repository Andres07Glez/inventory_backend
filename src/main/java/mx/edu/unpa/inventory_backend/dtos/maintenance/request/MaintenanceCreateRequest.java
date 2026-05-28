package mx.edu.unpa.inventory_backend.dtos.maintenance.request;

import jakarta.validation.constraints.*;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Payload para registrar un nuevo mantenimiento.
 * <p>
 * Todos los campos marcados como nullable en el schema (incidentId, performedBy,
 * cost, conditionBefore, conditionAfter) son opcionales aquí también.
 * </p>
 *
 * @param assetId         ID del bien al que pertenece el registro (obligatorio)
 * @param incidentId      ID de la incidencia vinculada (opcional)
 * @param maintenanceType Tipo de mantenimiento (obligatorio)
 * @param description     Descripción del trabajo realizado (obligatorio)
 * @param performedBy     Técnico o empresa responsable (opcional)
 * @param performedDate   Fecha real del servicio — no puede ser futura (obligatorio)
 * @param cost            Costo del servicio; null si es interno/garantía (opcional)
 * @param conditionBefore Condición del bien antes del servicio (opcional)
 * @param conditionAfter  Condición del bien después del servicio (opcional)
 */
public record MaintenanceCreateRequest(

        @NotNull(message = "El ID del bien es obligatorio")
        Long assetId,

        Long incidentId,

        @NotNull(message = "El tipo de mantenimiento es obligatorio")
        MaintenanceType maintenanceType,

        @NotBlank(message = "La descripción del trabajo es obligatoria")
        @Size(max = 5000, message = "La descripción no puede superar los 5000 caracteres")
        String description,

        @Size(max = 200, message = "El nombre del técnico no puede superar los 200 caracteres")
        String performedBy,

        @NotNull(message = "La fecha del servicio es obligatoria")
        @PastOrPresent(message = "La fecha del servicio no puede ser futura")
        LocalDate performedDate,

        @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
        @Digits(integer = 8, fraction = 2, message = "El costo debe tener máximo 8 dígitos enteros y 2 decimales")
        BigDecimal cost,

        ConditionStatus conditionBefore,

        ConditionStatus conditionAfter
) {}