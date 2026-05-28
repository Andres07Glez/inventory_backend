package mx.edu.unpa.inventory_backend.services;


import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentCloseRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentStatusUpdateDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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
    Page<IncidentSummaryDTO> list(IncidentStatus status, Long assetId, Pageable pageable);

    /** Todas las incidencias de un bien, sin paginar. Para el tab en el detalle del bien. */
    List<IncidentSummaryDTO> listByAsset(Long assetId);

    /**
     * Avanza el estado de una incidencia:
     *   OPEN → IN_PROGRESS
     *   IN_PROGRESS → RESOLVED
     *
     * Para cerrar la incidencia usar {@link #close} o {@link #confirmDecommission}.
     */
    IncidentResponseDTO updateStatus(Long id, IncidentStatusUpdateDTO dto);

    /**
     * Cierra la incidencia con resolución STANDARD (RESOLVED → CLOSED).
     * Accesible para ADMIN y OPERADOR.
     */
    IncidentResponseDTO close(Long id, IncidentCloseRequestDTO dto, Long closedById);

    /**
     * Cierra la incidencia con baja definitiva del bien (RESOLVED → CLOSED + DECOMMISSIONED).
     * Requiere rol ADMIN. Operación atómica: cierra incidencia y actualiza lifecycle del bien.
     *
     * @param id             ID de la incidencia
     * @param justification  Dictamen técnico obligatorio
     * @param document       Acta administrativa en PDF
     * @param adminId        ID del usuario ADMIN que confirma
     */
    IncidentResponseDTO confirmDecommission(Long id, String justification,
                                            MultipartFile document, Long adminId) throws IOException;
}