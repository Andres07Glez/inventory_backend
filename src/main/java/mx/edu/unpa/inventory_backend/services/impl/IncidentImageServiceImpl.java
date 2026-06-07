package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.domains.IncidentImage;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentImageResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.IncidentImageRepository;
import mx.edu.unpa.inventory_backend.repositories.IncidentRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.IncidentImageService;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class IncidentImageServiceImpl implements IncidentImageService {

    /** Máximo de imágenes de evidencia por incidencia. */
    private static final int     MAX_IMAGES    = 8;
    private static final long    MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final IncidentImageRepository imageRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<IncidentImageResponseDTO> getByIncidentId(Long incidentId) {
        requireIncident(incidentId);
        return imageRepository.findByIncidentIdOrdered(incidentId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public IncidentImageResponseDTO upload(Long incidentId, MultipartFile file, Long uploadedById)
            throws IOException {

        validateFile(file);

        long current = imageRepository.countByIncidentId(incidentId);
        if (current >= MAX_IMAGES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT,
                    "La incidencia ya tiene el máximo de " + MAX_IMAGES + " imágenes de evidencia.");
        }

        Incident incident  = requireIncident(incidentId);
        User uploadedBy = userRepository.findByIdAndIsActiveTrue(uploadedById)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + uploadedById));

        String relativePath = storageService.store(file, "incidents/" + incidentId + "/images");

        IncidentImage image = new IncidentImage();
        image.setIncident(incident);
        image.setFilePath(relativePath);
        image.setFileName(file.getOriginalFilename());
        image.setMimeType(file.getContentType());
        image.setUploadedBy(uploadedBy);

        return toDTO(imageRepository.save(image));
    }

    @Override
    @Transactional
    public void delete(Long incidentId, Long imageId) throws IOException {
        IncidentImage image = imageRepository.findByIdAndIncidentId(imageId, incidentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Imagen " + imageId + " no encontrada para la incidencia " + incidentId));

        String path = image.getFilePath();
        imageRepository.delete(image);
        storageService.delete(path);
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Formato no permitido. Solo se aceptan JPEG, PNG y WEBP.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "El archivo supera el tamaño máximo de 10 MB.");
        }
    }

    private Incident requireIncident(Long incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidencia no encontrada: " + incidentId));
    }

    private IncidentImageResponseDTO toDTO(IncidentImage img) {
        return new IncidentImageResponseDTO(
                img.getId(),
                img.getFileName(),
                storageService.buildPublicUrl(img.getFilePath()),
                img.getMimeType(),
                img.getUploadedAt(),
                img.getUploadedBy().getGuardian().getFullName()
        );
    }
}
