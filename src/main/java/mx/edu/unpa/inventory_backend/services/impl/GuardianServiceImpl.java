package mx.edu.unpa.inventory_backend.services.impl;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.dtos.guardian.request.GuardianRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.GuardianMapper;
import mx.edu.unpa.inventory_backend.repositories.GuardianRepository;
import mx.edu.unpa.inventory_backend.repositories.LocationRepository;
import mx.edu.unpa.inventory_backend.services.GuardianService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GuardianServiceImpl implements GuardianService {

    private final GuardianRepository  guardianRepository;
    private final LocationRepository locationRepository;
    private final GuardianMapper      guardianMapper;

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GuardianResponseDTO create(GuardianRequestDTO request) {

        if (guardianRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw new IllegalArgumentException(
                    "Ya existe un resguardante con el número de empleado: " + request.employeeNumber()
            );
        }

        Guardian guardian = guardianMapper.toEntity(request);
        guardian.setIsActive(true);
        guardian.setLocation(resolveLocation(request.locationId()));

        return guardianMapper.toDto(guardianRepository.save(guardian));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<GuardianResponseDTO> findAllActive(Pageable pageable) {
        return guardianRepository
                .findByIsActiveTrue(pageable)
                .map(guardianMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<GuardianResponseDTO> search(String q, Pageable pageable) {
        return guardianRepository
                .searchActive(q, pageable)
                .map(guardianMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public GuardianResponseDTO findById(Long id) {
        return guardianMapper.toDto(getOrThrow(id));
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GuardianResponseDTO update(Long id, GuardianRequestDTO request) {

        Guardian guardian = getOrThrow(id);

        // Valida unicidad solo si el número de empleado cambió
        if (!guardian.getEmployeeNumber().equals(request.employeeNumber())
                && guardianRepository.existsByEmployeeNumber(request.employeeNumber())) {
            throw new IllegalArgumentException(
                    "Ya existe un resguardante con el número de empleado: " + request.employeeNumber()
            );
        }

        guardianMapper.updateEntityFromDto(request, guardian);

        // Actualizar ubicación: resolveLocation devuelve null si locationId es null,
        // lo que permite limpiar la ubicación enviando null explícitamente.
        guardian.setLocation(resolveLocation(request.locationId()));

        return guardianMapper.toDto(guardianRepository.save(guardian));
    }

    // ── Baja lógica ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivate(Long id) {
        Guardian guardian = getOrThrow(id);
        guardian.setIsActive(false);
        guardianRepository.save(guardian);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Guardian getOrThrow(Long id) {
        return guardianRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Resguardante no encontrado con id: " + id));
    }

    /**
     * Resuelve la entidad Location a partir del ID.
     * Retorna null si locationId es null (resguardante sin ubicación base definida).
     */
    private Location resolveLocation(Integer locationId) {
        if (locationId == null) return null;
        return locationRepository.findByIdAndIsActiveTrue(locationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ubicación no encontrada o inactiva con id: " + locationId));
    }
}