package mx.edu.unpa.inventory_backend.services;


import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentCloseRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentStatusUpdateDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * REFACTORIZACIÓN SP-16:
 *   Se eliminó confirmDecommission(). La baja de un bien ahora es responsabilidad
 *   de DecommissionService, completamente independiente de este módulo.
 */
public interface IncidentService {

    /** Abre una nueva incidencia. El bien debe existir y no estar dado de baja. */
    IncidentResponseDTO create(IncidentRequestDTO request, Long createdById);

    /** Detalle completo de una incidencia con sus imágenes de evidencia. */
    IncidentResponseDTO getById(Long id);

    /**
     * Listado global paginado con filtros opcionales.
     * @param status  filtra por estado; null = todos
     * @param assetId filtra por bien;   null = todos
     */
    Page<IncidentSummaryDTO> list(IncidentStatus status, Long assetId,
                                  String folioQuery, Pageable pageable);

    /** Todas las incidencias de un bien, sin paginar. Para el tab en el detalle del bien. */
    List<IncidentSummaryDTO> listByAsset(Long assetId);

    /**
     * Avanza el estado de una incidencia:
     *   OPEN        → IN_PROGRESS
     *   IN_PROGRESS → RESOLVED
     *
     * Para cerrar la incidencia usar {@link #close}.
     */
    IncidentResponseDTO updateStatus(Long id, IncidentStatusUpdateDTO dto);

    /**
     * Cierra la incidencia (RESOLVED → CLOSED, closureType = STANDARD).
     * Accesible para ADMIN y OPERADOR.
     *
     * Si la resolución requirió una baja del bien, esa baja se registra
     * por separado en POST /v1/decommissions con el incidentId opcional.
     */
    IncidentResponseDTO close(Long id, IncidentCloseRequestDTO dto, Long closedById);
}