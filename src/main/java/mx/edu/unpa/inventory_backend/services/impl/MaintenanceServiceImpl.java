package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.domains.MaintenanceLog;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.maintenance.request.MaintenanceCreateRequest;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceResponse;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceSummary;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;
import mx.edu.unpa.inventory_backend.exceptions.InvalidAssetStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.IncidentRepository;
import mx.edu.unpa.inventory_backend.repositories.MaintenanceLogRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.MaintenanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MaintenanceServiceImpl implements MaintenanceService {

    private final MaintenanceLogRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    // ── Escritura ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public MaintenanceResponse create(MaintenanceCreateRequest request, Long createdById) {
        Asset asset     = requireAssetExists(request.assetId());
        User createdBy = requireActiveUser(createdById);
        Incident incident = resolveIncident(request.incidentId(), request.assetId());

        MaintenanceLog log = new MaintenanceLog();
        log.setAsset(asset);
        log.setIncident(incident);
        log.setCreatedBy(createdBy);
        log.setMaintenanceType(request.maintenanceType());
        log.setDescription(request.description());
        log.setPerformedBy(request.performedBy());
        log.setPerformedDate(request.performedDate());
        log.setCost(request.cost());
        log.setConditionBefore(request.conditionBefore());
        log.setConditionAfter(request.conditionAfter());

        return toResponse(maintenanceRepository.save(log));
    }

    // ── Lectura ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceSummary> getByAssetId(Long assetId) {
        requireAssetExists(assetId);
        return maintenanceRepository
                .findByAssetIdOrdered(assetId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MaintenanceSummary> getAll(MaintenanceType type) {
        return maintenanceRepository
                .findAllFiltered(type)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MaintenanceResponse getById(Long id) {
        return maintenanceRepository
                .findByIdWithDetails(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de mantenimiento no encontrado: " + id));
    }

    // ── Helpers de validación ───────────────────────────────────────────────────

    private Asset requireAssetExists(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado: " + assetId));
    }

    private User requireActiveUser(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));
    }

    /**
     * Resuelve la incidencia vinculada aplicando las reglas de negocio:
     * <ul>
     *   <li>Si {@code incidentId} es null → retorna null (sin vinculación, comportamiento normal)</li>
     *   <li>Si {@code incidentId} existe → valida que pertenezca al mismo bien</li>
     * </ul>
     *
     * <p><b>Edge case:</b> si la incidencia fue borrada (SET NULL en FK) el id puede existir en el
     * request pero no en BD — se lanza 404 en lugar de FK violation.</p>
     */
    private Incident resolveIncident(Long incidentId, Long assetId) {
        if (incidentId == null) {
            return null;
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada: " + incidentId));

        if (!incident.getAsset().getId().equals(assetId)) {
            throw new InvalidAssetStateException(
                    "La incidencia %d no pertenece al bien %d.".formatted(incidentId, assetId)
            );
        }

        return incident;
    }

    // Agrega esta implementación
    @Override
    @Transactional
    public void delete(Long id) {
        // Validamos que el registro exista antes de intentar borrarlo
        if (!maintenanceRepository.existsById(id)) {
            throw new ResourceNotFoundException("No se puede eliminar: Registro de mantenimiento no encontrado con ID: " + id);
        }
        maintenanceRepository.deleteById(id);
    }

    // ── Mappers ─────────────────────────────────────────────────────────────────

    private MaintenanceResponse toResponse(MaintenanceLog log) {
        return new MaintenanceResponse(
                log.getId(),
                log.getAsset().getId(),
                log.getAsset().getInventoryNumber(),
                log.getAsset().getDescription(),
                log.getIncident() != null ? log.getIncident().getId() : null,
                log.getMaintenanceType(),
                log.getDescription(),
                log.getPerformedBy(),
                log.getPerformedDate(),
                log.getCost(),
                log.getConditionBefore(),
                log.getConditionAfter(),
                log.getCreatedAt(),
                log.getCreatedBy().getFullName()
        );
    }

    private MaintenanceSummary toSummary(MaintenanceLog log) {
        return new MaintenanceSummary(
                log.getId(),
                log.getAsset().getId(),
                log.getAsset().getInventoryNumber(),
                log.getIncident() != null ? log.getIncident().getId() : null,
                log.getMaintenanceType(),
                log.getPerformedBy(),
                log.getPerformedDate(),
                log.getCost(),
                log.getConditionBefore(),
                log.getConditionAfter(),
                log.getCreatedBy().getFullName()
        );
    }
}
