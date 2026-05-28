package mx.edu.unpa.inventory_backend.services.impl;


import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.dtos.location.request.LocationRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.location.response.LocationResponseDTO;
import mx.edu.unpa.inventory_backend.mappers.LocationMapper;
import mx.edu.unpa.inventory_backend.repositories.LocationRepository;
import mx.edu.unpa.inventory_backend.services.LocationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    private final LocationRepository locationRepository;
    private final LocationMapper locationMapper;

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LocationResponseDTO create(LocationRequestDTO request) {

        // Valida duplicado por nombre + campus (combinación que debe ser única)
        if (request.campus() != null
                && locationRepository.existsByNameAndCampus(request.name(), request.campus())) {
            throw new IllegalArgumentException(
                    "Ya existe una ubicación con el nombre '"
                            + request.name() + "' en el campus '" + request.campus() + "'"
            );
        }

        Location location = locationMapper.toEntity(request);
        location.setIsActive(true);

        return locationMapper.toDto(locationRepository.save(location));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponseDTO> findAllActive(Pageable pageable) {
        return locationRepository
                .findByIsActiveTrue(pageable)
                .map(locationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LocationResponseDTO> search(String q, Pageable pageable) {
        return locationRepository
                .searchActive(q, pageable)
                .map(locationMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public LocationResponseDTO findById(Long id) {
        return locationMapper.toDto(getOrThrow(id));
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public LocationResponseDTO update(Long id, LocationRequestDTO request) {

        Location location = getOrThrow(id);

        // Valida duplicado solo si cambió el nombre o el campus
        boolean nameChanged   = !location.getName().equals(request.name());
        boolean campusChanged = request.campus() != null
                && !request.campus().equals(location.getCampus());

        if ((nameChanged || campusChanged)
                && request.campus() != null
                && locationRepository.existsByNameAndCampus(request.name(), request.campus())) {
            throw new IllegalArgumentException(
                    "Ya existe una ubicación con el nombre '"
                            + request.name() + "' en el campus '" + request.campus() + "'"
            );
        }

        locationMapper.updateEntityFromDto(request, location);
        return locationMapper.toDto(locationRepository.save(location));
    }

    // ── Baja lógica ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivate(Long id) {
        Location location = getOrThrow(id);
        location.setIsActive(false);
        locationRepository.save(location);
    }

    // ── Helper privado ────────────────────────────────────────────────────────

    private Location getOrThrow(Long id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ubicación no encontrada con id: " + id));
    }
}
