package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetAssignment;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentCloseRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentStatusUpdateDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentImageResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.ClosureType;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.exceptions.InvalidIncidentStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetAssignmentRepository;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.IncidentRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.IncidentService;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Year;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * REFACTORIZACIÓN SP-16:
 *   Se eliminó confirmDecommission() y toda su lógica de baja (storage de PDF,
 *   actualización de lifecycle_status, etc.).
 *
 *   La única responsabilidad de este servicio es gestionar el ciclo de vida
 *   de las incidencias: OPEN → IN_PROGRESS → RESOLVED → CLOSED.
 */
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private static final LocalDate MIN_INCIDENT_DATE = LocalDate.of(2002, 1, 1);

    /** Patrón para extraer el ID numérico de un folio: INC-2026-00042 → 42 */
    private static final Pattern FOLIO_PATTERN =
            Pattern.compile("^INC-\\d{4}-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final IncidentRepository incidentRepository;
    private final AssetRepository    assetRepository;
    private final UserRepository     userRepository;
    private final StorageService     storageService;
    private final AssetAssignmentRepository assetAssignmentRepository;

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public IncidentResponseDTO create(IncidentRequestDTO request, Long createdById) {

        Asset asset = requireAsset(request.assetId());

        if (asset.getLifecycleStatus() == LifecycleStatus.DECOMMISSIONED) {
            throw new InvalidIncidentStateException(
                    "No se puede reportar una incidencia sobre un bien dado de baja.");
        }

        User createdBy = requireUser(createdById);

        // Fecha de la incidencia: usar la proporcionada o la fecha actual
        LocalDate incidentDate = request.incidentDate() != null
                ? request.incidentDate()
                : LocalDate.now();

        // Edge case: fecha mínima permitida
        if (incidentDate.isBefore(MIN_INCIDENT_DATE)) {
            throw new InvalidIncidentStateException(
                    "La fecha de la incidencia no puede ser anterior al año 2002.");
        }

        Incident incident = new Incident();
        incident.setAsset(asset);
        incident.setIncidentDate(incidentDate);
        incident.setDescription(request.description());
        incident.setConditionAtIncident(asset.getConditionStatus());
        incident.setRepairType(request.repairType());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedBy(createdBy);

        return toResponseDTO(incidentRepository.save(incident));
    }

    // ── Consultar ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public IncidentResponseDTO getById(Long id) {
        return toResponseDTO(requireIncidentWithDetails(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<IncidentSummaryDTO> list(IncidentStatus status, Long assetId,
                                         String folioQuery, Pageable pageable) {
        Long idFromFolio = extractIdFromFolio(folioQuery);
        return incidentRepository
                .findAllFiltered(status, assetId, idFromFolio, pageable)
                .map(this::toSummaryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IncidentSummaryDTO> listByAsset(Long assetId) {
        requireAsset(assetId);
        return incidentRepository.findAllByAssetId(assetId)
                .stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    // ── Cambio de estado ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public IncidentResponseDTO updateStatus(Long id, IncidentStatusUpdateDTO dto) {
        Incident incident = requireIncidentWithDetails(id);

        validateTransition(incident.getStatus(), dto.status());

        // Efecto de negocio: al pasar a IN_PROGRESS, el bien entra a mantenimiento
        if (dto.status() == IncidentStatus.IN_PROGRESS) {
            returnAssetToInventory(incident.getAsset());
        }

        incident.setStatus(dto.status());

        return toResponseDTO(incidentRepository.save(incident));
    }

    // ── Cierre STANDARD ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public IncidentResponseDTO close(Long id, IncidentCloseRequestDTO dto, Long closedById) {
        Incident incident = requireIncidentWithDetails(id);

        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new InvalidIncidentStateException(
                    "Solo se puede cerrar una incidencia en estado RESOLVED. " +
                            "Estado actual: " + incident.getStatus());
        }

        User closedBy = requireUser(closedById);

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setResolutionNotes(dto.resolutionNotes());
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(closedBy);

        /*if (dto.repairType() != null) {
            incident.setRepairType(dto.repairType());
        }*/

        return toResponseDTO(incidentRepository.save(incident));
    }

    // ── Helpers de mapeo ──────────────────────────────────────────────────────

    private IncidentResponseDTO toResponseDTO(Incident i) {
        List<IncidentImageResponseDTO> imageDTOs = i.getImages().stream()
                .map(img -> new IncidentImageResponseDTO(
                        img.getId(),
                        img.getFileName(),
                        storageService.buildPublicUrl(img.getFilePath()),
                        img.getMimeType(),
                        img.getUploadedAt(),
                        img.getUploadedBy().getFullName()))
                .toList();

        return new IncidentResponseDTO(
                i.getId(),
                buildFolio(i.getId()),
                i.getAsset().getId(),
                i.getAsset().getInventoryNumber(),
                i.getAsset().getDescription(),
                i.getDescription(),
                i.getRepairType(),
                i.getStatus(),
                i.getConditionAtIncident(),
                i.getIncidentDate(),
                i.getResolutionNotes(),
                i.getResolvedAt(),
                i.getResolvedBy() != null ? i.getResolvedBy().getFullName() : null,
                i.getCreatedAt(),
                i.getCreatedBy().getFullName(),
                imageDTOs
        );
    }

    private IncidentSummaryDTO toSummaryDTO(Incident i) {
        return new IncidentSummaryDTO(
                i.getId(),
                buildFolio(i.getId()),
                i.getDescription(),
                i.getStatus(),
                i.getConditionAtIncident(),
                i.getRepairType(),
                i.getCreatedAt(),
                i.getIncidentDate(),
                i.getCreatedBy().getFullName()
        );
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    static String buildFolio(Long incidentId) {
        return "INC-" + Year.now().getValue() + "-" + String.format("%05d", incidentId);
    }

    /* Edge case: el año en el folio se ignora deliberadamente — un admin puede
     * buscar INC-2025-00042 estando en 2026 y debe encontrar el registro porque
     * el folio se calcula con Year.now() en Java, no se almacena en DB.
            * Para que la búsqueda sea consistente se filtra solo por el ID numérico.
     */
    static Long extractIdFromFolio(String folioQuery) {
        if (folioQuery == null || folioQuery.isBlank()) return null;
        Matcher m = FOLIO_PATTERN.matcher(folioQuery.trim());
        if (!m.matches()) return null;
        try {
            return Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Transiciones permitidas en el flujo normal de incidencias:
     *   OPEN        → IN_PROGRESS
     *   IN_PROGRESS → RESOLVED
     *
     * RESOLVED → CLOSED se gestiona exclusivamente en /close.
     */
    private void validateTransition(IncidentStatus current, IncidentStatus next) {
        boolean valid = switch (current) {
            case OPEN        -> next == IncidentStatus.IN_PROGRESS;
            case IN_PROGRESS -> next == IncidentStatus.RESOLVED;
            case RESOLVED, CLOSED -> false;
        };

        if (!valid) {
            throw new InvalidIncidentStateException(
                    "Transición de estado inválida: " + current + " → " + next +
                            ". Para cerrar la incidencia usa el endpoint /close.");
        }
    }

    private Incident requireIncidentWithDetails(Long id) {
        return incidentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Incidencia no encontrada: " + id));
    }

    private Asset requireAsset(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Bien no encontrado: " + assetId));
    }

    private User requireUser(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado: " + userId));
    }

    /**
     * Retira el bien de su resguardante actual y lo marca como IN_MAINTENANCE.
     *
     * Edge cases manejados:
     *  - Bien sin asignación activa: no hay nada que cerrar, solo se actualiza el lifecycle.
     *  - Bien ya en IN_MAINTENANCE: idempotente, no lanza excepción.
     */
    private void returnAssetToInventory(Asset asset) {
        // Edge case 1 & 3: puede no existir asignación activa (dato limpio o sucio)
        Optional<AssetAssignment> activeAssignment =
                assetAssignmentRepository.findActiveByAssetId(asset.getId());

        activeAssignment.ifPresent(assignment -> {
            assignment.setReturnedAt(LocalDateTime.now());
            assetAssignmentRepository.save(assignment);
        });

        // Edge case 2: idempotente — si ya está IN_MAINTENANCE no hace daño volver a setearlo
        asset.setLifecycleStatus(LifecycleStatus.IN_MAINTENANCE);
        // El location_id del asset NO se toca; el bien físicamente está en reparación,
        // no "en ningún lado". Si tu dominio requiere borrar la ubicación, ajusta aquí.
        assetRepository.save(asset);
    }
}
