package mx.edu.unpa.inventory_backend.dtos.decommission.response;

import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Vista resumida para el listado paginado de bajas.
 * GET /v1/decommissions
 */
public record DecommissionSummaryDTO(
        Long id,
        Long assetId,
        String assetInventoryNumber,
        String assetDescription,
        DecommissionStatus status,
        LocalDate decommissionDate,
        LocalDateTime createdAt,
        String createdByName,
        /** true si esta baja está vinculada a una incidencia */
        boolean hasLinkedIncident
) {}