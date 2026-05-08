package mx.edu.unpa.inventory_backend.dtos.asset.request;

import jakarta.validation.constraints.NotNull;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;

public record UpdateConditionRequest(

        @NotNull(message = "El estado de condición es obligatorio")
        ConditionStatus conditionStatus

) {}
