package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.location.request.LocationRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.location.response.LocationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LocationService {

    /** Crea una nueva ubicación. Lanza excepción si ya existe el mismo nombre en el mismo campus. */
    LocationResponseDTO create(LocationRequestDTO request);

    /** Retorna la página de ubicaciones activas. */
    Page<LocationResponseDTO> findAllActive(Pageable pageable);

    /**
     * Búsqueda por nombre, edificio o campus.
     *
     * @param q        texto a buscar
     * @param pageable paginación
     */
    Page<LocationResponseDTO> search(String q, Pageable pageable);

    /** Busca una ubicación por ID. Lanza EntityNotFoundException si no existe. */
    LocationResponseDTO findById(Long id);

    /** Actualiza los datos de una ubicación existente. */
    LocationResponseDTO update(Long id, LocationRequestDTO request);

    /**
     * Desactiva una ubicación (baja lógica).
     * No elimina el registro para preservar el historial de bienes y asignaciones.
     */
    void deactivate(Long id);
}