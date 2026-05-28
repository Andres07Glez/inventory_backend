package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Asset;
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

import java.time.Year;
import java.io.IOException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private static final Set<String> ALLOWED_PDF = Set.of("application/pdf");

    private final IncidentRepository  incidentRepository;
    private final AssetRepository     assetRepository;
    private final UserRepository      userRepository;
    private final StorageService storageService;

    // ── Crear ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public IncidentResponseDTO create(IncidentRequestDTO request, Long createdById) {

        Asset asset = requireAsset(request.assetId());

        if (asset.getLifecycleStatus() == LifecycleStatus.DECOMMISSIONED) {
            throw new InvalidIncidentStateException(
                    "No se puede reportar una incidencia sobre un bien dado de baja.");
        }

        User createdBy = requireUser(createdById);

        Incident incident = new Incident();
        incident.setAsset(asset);
        incident.setDescription(request.description());
        incident.setConditionAtIncident(request.conditionAtIncident());
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
    public Page<IncidentSummaryDTO> list(IncidentStatus status, Long assetId, Pageable pageable) {
        return incidentRepository
                .findAllFiltered(status, assetId, pageable)
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

        incident.setStatus(dto.status());

        if (dto.resolutionNotes() != null && !dto.resolutionNotes().isBlank()) {
            incident.setResolutionNotes(dto.resolutionNotes());
        }
        if (dto.repairType() != null) {
            incident.setRepairType(dto.repairType());
        }

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
        incident.setClosureType(ClosureType.STANDARD);
        incident.setResolutionNotes(dto.resolutionNotes());
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(closedBy);

        if (dto.repairType() != null) {
            incident.setRepairType(dto.repairType());
        }

        return toResponseDTO(incidentRepository.save(incident));
    }

    // ── Cierre con BAJA DEFINITIVA (solo ADMIN) ───────────────────────────────

    @Override
    @Transactional
    public IncidentResponseDTO confirmDecommission(Long id, String justification,
                                                   MultipartFile document, Long adminId) throws IOException {

        Incident incident = requireIncidentWithDetails(id);

        if (incident.getStatus() != IncidentStatus.RESOLVED) {
            throw new InvalidIncidentStateException(
                    "La baja definitiva solo puede confirmarse desde estado RESOLVED. " +
                            "Estado actual: " + incident.getStatus());
        }

        validatePdf(document);

        if (justification == null || justification.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El dictamen técnico es obligatorio para procesar una baja definitiva.");
        }

        User admin = requireUser(adminId);

        // Almacenar acta PDF
        String subDir      = "incidents/" + id + "/docs";
        String documentPath = storageService.store(document, subDir);

        // Cerrar incidencia
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setClosureType(ClosureType.DECOMMISSION);
        incident.setDecommissionJustification(justification);
        incident.setDecommissionDocumentPath(documentPath);
        incident.setResolvedAt(LocalDateTime.now());
        incident.setResolvedBy(admin);

        // Dar de baja el bien — operación atómica dentro de la misma transacción
        Asset asset = incident.getAsset();
        asset.setLifecycleStatus(LifecycleStatus.DECOMMISSIONED);
        assetRepository.save(asset);

        return toResponseDTO(incidentRepository.save(incident));
    }

    // ── Helpers de mapeo ──────────────────────────────────────────────────────

    private IncidentResponseDTO toResponseDTO(Incident i) {
        String docUrl = null;
        if (i.getDecommissionDocumentPath() != null) {
            docUrl = storageService.buildPublicUrl(i.getDecommissionDocumentPath());
        }

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
                i.getResolutionNotes(),
                i.getResolvedAt(),
                i.getResolvedBy() != null ? i.getResolvedBy().getFullName() : null,
                i.getClosureType(),
                i.getDecommissionJustification(),
                docUrl,
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
                i.getClosureType(),
                i.getCreatedAt(),
                i.getCreatedBy().getFullName()
        );
    }

    // ── Utilidades ────────────────────────────────────────────────────────────

    /**
     * Folio legible. Ejemplo: INC-2026-00003.
     * Nota: usa el ID de la BD, no el año de creación del registro.
     * Si se necesita el año de la incidencia, adaptar pasando i.getCreatedAt().getYear().
     */
    static String buildFolio(Long incidentId) {
        return "INC-" + Year.now().getValue() + "-" + String.format("%05d", incidentId);
    }

    /**
     * Valida que la transición de estado sea la correcta en el flujo.
     * Tabla de transiciones permitidas para este endpoint:
     *   OPEN        → IN_PROGRESS
     *   IN_PROGRESS → RESOLVED
     *
     * RESOLVED → CLOSED se gestiona exclusivamente en /close y /decommission.
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
                            ". Para cerrar una incidencia usa los endpoints /close o /decommission.");
        }
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El acta administrativa en PDF es obligatoria.");
        }
        if (!ALLOWED_PDF.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Solo se acepta un archivo PDF como acta de baja.");
        }
    }

    private Incident requireIncidentWithDetails(Long id) {
        return incidentRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada: " + id));
    }

    private Asset requireAsset(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado: " + assetId));
    }

    private User requireUser(Long userId) {
        return userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + userId));
    }
}
