package mx.edu.unpa.inventory_backend.storage.impl;

import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.storage.StorageProperties;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileSystemStorageServiceImpl implements StorageService {

    private final Path uploadRoot;
    private final StorageProperties props;

    public LocalFileSystemStorageServiceImpl(StorageProperties props) {
        this.props = props;
        this.uploadRoot = Paths.get(props.uploadDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadRoot);
            log.info("Directorio de almacenamiento: {}", uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de almacenamiento: " + uploadRoot, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        validateFile(file);

        String extension  = getExtension(file.getOriginalFilename());
        String uniqueName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relative   = subDir + "/" + uniqueName;
        Path   target     = uploadRoot.resolve(relative).normalize();

        // Evita path traversal
        if (!target.startsWith(uploadRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ruta de archivo inválida");
        }

        try {
            Files.createDirectories(target.getParent());
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            log.debug("Archivo guardado: {}", relative);
            return relative;
        } catch (IOException e) {
            log.error("Error al guardar archivo en {}: {}", relative, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar el archivo en el servidor");
        }
    }

    @Override
    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path target = uploadRoot.resolve(relativePath).normalize();
            if (!target.startsWith(uploadRoot)) return; // seguridad: ignorar rutas extrañas
            Files.deleteIfExists(target);
            log.debug("Archivo eliminado: {}", relativePath);
        } catch (IOException e) {
            // Loguear pero no relanzar — la eliminación del archivo no debe bloquear la operación de negocio
            log.warn("No se pudo eliminar el archivo {}: {}", relativePath, e.getMessage());
        }
    }

    @Override
    public String buildPublicUrl(String relativePath) {
        return props.baseUrl() + "/uploads/" + relativePath;
    }

    // ── Validaciones ──────────────────────────────────────────────────────────

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no puede estar vacío");
        }
        String mimeType = file.getContentType();
        if (mimeType == null || !props.allowedMimeTypes().contains(mimeType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Tipo de archivo no permitido. Usa JPEG, PNG o WebP.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
