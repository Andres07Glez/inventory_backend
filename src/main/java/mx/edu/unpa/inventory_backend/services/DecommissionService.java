package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.decommission.request.DecommissionRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface DecommissionService {

    /**
     * Inicia un proceso de baja (estado PENDING).
     * El bien puede o no tener una incidencia relacionada.
     * Accesible para ADMIN y OPERADOR.
     *
     * @param request       datos de la baja (assetId obligatorio, incidentId opcional)
     * @param document      acta PDF opcional (puede ser null)
     * @param createdById   ID del usuario que inicia la baja
     */
    DecommissionResponseDTO create(DecommissionRequestDTO request,
                                   MultipartFile document,
                                   Long createdById) throws IOException;

    /**
     * Confirma la baja definitiva (PENDING → CONFIRMED).
     * Marca el bien como DECOMMISSIONED de forma atómica.
     * Solo ADMIN puede ejecutar esta operación.
     *
     * @param id          ID de la baja a confirmar
     * @param adminId     ID del usuario ADMIN que confirma
     */
    DecommissionResponseDTO confirm(Long id, Long adminId);

    /**
     * Detalle completo de una baja.
     */
    DecommissionResponseDTO getById(Long id);

    /**
     * Listado paginado con filtro opcional por estado.
     *
     */
    Page<DecommissionSummaryDTO> list(DecommissionStatus status, Pageable pageable);

    /**
     * Baja asociada a un bien específico (si existe).
     * Para mostrar el panel de baja en el detalle del bien.
     *
     * @throws mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException
     *         si el bien no existe o no tiene baja registrada
     */
    DecommissionResponseDTO getByAssetId(Long assetId);
}