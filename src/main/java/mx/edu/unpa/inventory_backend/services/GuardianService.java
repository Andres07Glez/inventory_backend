package mx.edu.unpa.inventory_backend.services;


import mx.edu.unpa.inventory_backend.dtos.guardian.request.GuardianRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GuardianService {

    /** Crea un nuevo resguardante. Lanza excepción si el número de empleado ya existe. */
    GuardianResponseDTO create(GuardianRequestDTO request);

    /** Retorna la página de resguardantes activos. */
    Page<GuardianResponseDTO> findAllActive(Pageable pageable);

    /**
     * Búsqueda por nombre, número de empleado o departamento.
     *
     * @param q        texto a buscar
     * @param pageable paginación
     */
    Page<GuardianResponseDTO> search(String q, Pageable pageable);

    /** Busca un resguardante por ID. Lanza EntityNotFoundException si no existe. */
    GuardianResponseDTO findById(Long id);

    /** Actualiza los datos de un resguardante existente. */
    GuardianResponseDTO update(Long id, GuardianRequestDTO request);

    /**
     * Desactiva un resguardante (baja lógica).
     * No elimina el registro para preservar el historial de asignaciones.
     */
    void deactivate(Long id);
}