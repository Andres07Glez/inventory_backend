package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetImage;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.image.response.AssetImageResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetImageRepository;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.AssetImageService;
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
public class AssetImageServiceImpl implements AssetImageService {

    private static final int    MAX_IMAGES        = 5;
    private static final long   MAX_FILE_SIZE     = 10 * 1024 * 1024L; // 10 MB
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp");

    private final AssetImageRepository imageRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final StorageService storageService;

    @Override
    @Transactional(readOnly = true)
    public List<AssetImageResponseDTO> getByAssetId(Long assetId) {
        requireAssetExists(assetId);
        return imageRepository
                .findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(assetId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public AssetImageResponseDTO upload(Long assetId, MultipartFile file, Long uploadedById) throws IOException {
        // ── Validaciones ────────────────────────────────────────────────────
        validateFile(file);

        long current = imageRepository.countByAssetId(assetId);
        if (current >= MAX_IMAGES) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El bien ya tiene el máximo de " + MAX_IMAGES + " imágenes permitidas.");
        }

        Asset asset      = requireAssetExists(assetId);
        User uploadedBy = userRepository.findByIdAndIsActiveTrue(uploadedById)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + uploadedById));

        // ── Almacenar archivo ────────────────────────────────────────────────
        String subDir;
        String relativePath;
        subDir       = "assets/" + assetId;
        relativePath = storageService.store(file, subDir);

        // ── Persistir registro ───────────────────────────────────────────────
        AssetImage image = new AssetImage();
        image.setAsset(asset);
        image.setFilePath(relativePath);
        image.setFileName(file.getOriginalFilename());
        image.setMimeType(file.getContentType());
        image.setUploadedBy(uploadedBy);
        // Primera imagen → primaria automáticamente
        image.setIsPrimary(current == 0);

        return toDTO(imageRepository.save(image));
    }

    @Override
    @Transactional
    public void delete(Long assetId, Long imageId) throws IOException {
        AssetImage image = imageRepository.findByIdAndAssetId(imageId, assetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Imagen " + imageId + " no encontrada para el bien " + assetId));

        boolean wasPrimary = Boolean.TRUE.equals(image.getIsPrimary());
        String  path       = image.getFilePath();

        imageRepository.delete(image);

        // Si era la primaria, promover la imagen más antigua restante
        if (wasPrimary) {
            imageRepository
                    .findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(assetId)
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsPrimary(true);
                        imageRepository.save(next);
                    });
        }

        // Eliminar archivo físico (no lanzar excepción si ya no existe)
        storageService.delete(path);
    }

    @Override
    @Transactional
    public AssetImageResponseDTO setPrimary(Long assetId, Long imageId) {
        requireAssetExists(assetId);
        AssetImage image = imageRepository.findByIdAndAssetId(imageId, assetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Imagen " + imageId + " no encontrada para el bien " + assetId));

        imageRepository.clearPrimaryByAssetId(assetId);
        image.setIsPrimary(true);
        return toDTO(imageRepository.save(image));
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
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "El archivo supera el tamaño máximo de 10 MB.");
        }
    }

    private Asset requireAssetExists(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Bien no encontrado: " + assetId));
    }

    private AssetImageResponseDTO toDTO(AssetImage img) {
        return new AssetImageResponseDTO(
                img.getId(),
                img.getFileName(),
                storageService.buildPublicUrl(img.getFilePath()),
                img.getMimeType(),
                Boolean.TRUE.equals(img.getIsPrimary())
        );
    }
}
