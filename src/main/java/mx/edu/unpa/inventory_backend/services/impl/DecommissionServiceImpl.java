package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetDecommission;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.decommission.request.DecommissionRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.exceptions.InvalidDecommissionStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.DecommissionRepository;
import mx.edu.unpa.inventory_backend.repositories.IncidentRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.DecommissionService;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DecommissionServiceImpl implements DecommissionService {

    private static final Set<String> ALLOWED_PDF   = Set.of("application/pdf");
    private static final long        MAX_PDF_SIZE  = 20 * 1024 * 1024L; // 20 MB

    private final DecommissionRepository decommissionRepository;
    private final AssetRepository assetRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    // ── Crear baja (PENDING) ──────────────────────────────────────────────────

    @Override
    @Transactional
    public DecommissionResponseDTO create(DecommissionRequestDTO request,
                                          MultipartFile document,
                                          Long createdById) throws IOException {

        Asset asset = requireAsset(request.assetId());
        validateAssetCanBeDecommissioned(asset);

        // Edge case: no crear segunda baja para el mismo bien
        if (decommissionRepository.existsByAssetId(asset.getId())) {
            throw new InvalidDecommissionStateException(
                    "El bien " + asset.getInventoryNumber() +
                            " ya tiene un proceso de baja registrado.");
        }

        User createdBy = requireUser(createdById);

        // Relación opcional con incidencia
        Incident incident = resolveOptionalIncident(request.incidentId(), asset);

        // Documento PDF opcional
        String documentPath = null;
        if (document != null && !document.isEmpty()) {
            validatePdf(document);
            documentPath = storageService.store(document,
                    "decommissions/" + asset.getId() + "/docs");
        }

        AssetDecommission decommission = new AssetDecommission();
        decommission.setAsset(asset);
        decommission.setIncident(incident);
        decommission.setJustification(request.justification());
        decommission.setDocumentPath(documentPath);
        decommission.setDecommissionDate(
                request.decommissionDate() != null ? request.decommissionDate() : LocalDate.now());
        decommission.setStatus(DecommissionStatus.PENDING);
        decommission.setCreatedBy(createdBy);

        return toResponseDTO(decommissionRepository.save(decommission));
    }

    // ── Confirmar baja definitiva (PENDING → CONFIRMED) ───────────────────────

    @Override
    @Transactional
    public DecommissionResponseDTO confirm(Long id, Long adminId) {

        AssetDecommission decommission = requireDecommissionWithDetails(id);

        if (decommission.getStatus() == DecommissionStatus.CONFIRMED) {
            throw new InvalidDecommissionStateException(
                    "La baja " + id + " ya está confirmada. No se puede confirmar dos veces.");
        }

        User admin = requireUser(adminId);

        // Dar de baja el bien — operación atómica dentro de la misma transacción
        Asset asset = decommission.getAsset();
        asset.setLifecycleStatus(LifecycleStatus.DECOMMISSIONED);
        assetRepository.save(asset);

        decommission.setStatus(DecommissionStatus.CONFIRMED);
        decommission.setConfirmedBy(admin);
        decommission.setConfirmedAt(LocalDateTime.now());

        return toResponseDTO(decommissionRepository.save(decommission));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public DecommissionResponseDTO getById(Long id) {
        return toResponseDTO(requireDecommissionWithDetails(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DecommissionSummaryDTO> list(DecommissionStatus status, Pageable pageable) {
        return decommissionRepository
                .findAllFiltered(status, pageable)
                .map(this::toSummaryDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public DecommissionResponseDTO getByAssetId(Long assetId) {
        requireAsset(assetId); // valida que el bien exista antes de buscar su baja
        return decommissionRepository.findByAssetId(assetId)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El bien " + assetId + " no tiene un proceso de baja registrado."));
    }

    // ── Mapeo ─────────────────────────────────────────────────────────────────

    private DecommissionResponseDTO toResponseDTO(AssetDecommission d) {
        String docUrl = d.getDocumentPath() != null
                ? storageService.buildPublicUrl(d.getDocumentPath())
                : null;

        String incidentFolio = null;
        Long   incidentId    = null;
        if (d.getIncident() != null) {
            incidentId    = d.getIncident().getId();
            incidentFolio = buildIncidentFolio(incidentId);
        }

        return new DecommissionResponseDTO(
                d.getId(),
                d.getAsset().getId(),
                d.getAsset().getInventoryNumber(),
                d.getAsset().getDescription(),
                incidentId,
                incidentFolio,
                d.getJustification(),
                docUrl,
                d.getDecommissionDate(),
                d.getStatus(),
                d.getCreatedAt(),
                d.getCreatedBy().getFullName(),
                d.getConfirmedAt(),
                d.getConfirmedBy() != null ? d.getConfirmedBy().getFullName() : null
        );
    }

    private DecommissionSummaryDTO toSummaryDTO(AssetDecommission d) {
        return new DecommissionSummaryDTO(
                d.getId(),
                d.getAsset().getId(),
                d.getAsset().getInventoryNumber(),
                d.getAsset().getDescription(),
                d.getStatus(),
                d.getDecommissionDate(),
                d.getCreatedAt(),
                d.getCreatedBy().getFullName(),
                d.getIncident() != null
        );
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    /**
     * Valida que el bien puede entrar en un proceso de baja.
     *
     * Edge cases cubiertos:
     *   - Bien ya dado de baja (DECOMMISSIONED): prohibido
     *   - Bien en cualquier otro estado (REGISTERED, AVAILABLE, ASSIGNED,
     *     IN_MAINTENANCE, IN_WARRANTY): permitido
     */
    private void validateAssetCanBeDecommissioned(Asset asset) {
        if (asset.getLifecycleStatus() == LifecycleStatus.DECOMMISSIONED) {
            throw new InvalidDecommissionStateException(
                    "El bien " + asset.getInventoryNumber() + " ya está dado de baja.");
        }
    }

    /**
     * Resuelve la incidencia opcional.
     *
     * Si incidentId es null → retorna null (baja directa, sin incidencia).
     * Si incidentId está presente → valida existencia y que corresponda al mismo bien.
     *
     * No se requiere que la incidencia esté en un estado particular:
     * el operador puede vincular una incidencia CLOSED, RESOLVED, o incluso OPEN
     * si el contexto del negocio lo requiere. Si se necesita restringir esto,
     * agregar la validación aquí con un Set<IncidentStatus> de estados permitidos.
     */
    private Incident resolveOptionalIncident(Long incidentId, Asset asset) {
        if (incidentId == null) {
            return null;
        }

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Incidencia no encontrada: " + incidentId));

        if (!incident.getAsset().getId().equals(asset.getId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La incidencia " + incidentId +
                            " no corresponde al bien " + asset.getInventoryNumber() + ".");
        }

        return incident;
    }

    private void validatePdf(MultipartFile file) {
        if (!ALLOWED_PDF.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Solo se acepta un archivo PDF como documento de baja.");
        }
        if (file.getSize() > MAX_PDF_SIZE) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El archivo supera el tamaño máximo de 20 MB.");
        }
    }

    /** Folio legible de incidencia para incluir en la respuesta de baja. */
    private static String buildIncidentFolio(Long incidentId) {
        return "INC-" + Year.now().getValue() + "-" + String.format("%05d", incidentId);
    }

    private AssetDecommission requireDecommissionWithDetails(Long id) {
        return decommissionRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proceso de baja no encontrado: " + id));
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
}
