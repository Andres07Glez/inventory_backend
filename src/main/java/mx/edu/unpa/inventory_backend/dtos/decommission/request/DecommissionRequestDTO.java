package mx.edu.unpa.inventory_backend.dtos.decommission.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * Payload para iniciar un proceso de baja de bien.
 * POST /v1/decommissions   (multipart/form-data — el documento PDF se envía aparte)
 *
 * El bien se selecciona previamente con GET /v1/assets/search.
 * La incidencia de origen es OPCIONAL.
 *
 * Nota de validación:
 *   Si incidentId está presente, el servicio valida que:
 *     1. La incidencia exista y esté en estado CLOSED o RESOLVED.
 *     2. El bien de la incidencia coincida con assetId.
 *   Estos son edge cases de integridad; no se validan con anotaciones Jakarta
 *   porque requieren acceso a la base de datos.
 */
public record DecommissionRequestDTO(

        @NotNull(message = "El ID del bien es obligatorio")
        @Positive(message = "El ID del bien debe ser positivo")
        Long assetId,

        /**
         * ID de incidencia relacionada. NULL si la baja es independiente.
         * Si se provee, el servicio valida que sea una incidencia válida y del mismo bien.
         */
        Long incidentId,

        @NotBlank(message = "La justificación de la baja es obligatoria")
        String justification,

        /**
         * Fecha oficial de la baja. Si es null, el servicio usa LocalDate.now().
         * Permite registrar bajas con fecha retroactiva (auditoría de inventario físico).
         */
        LocalDate decommissionDate
) {}
