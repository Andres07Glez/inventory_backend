package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentImageResponseDTO;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.IncidentImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/incidents/{incidentId}/images")
@RequiredArgsConstructor
public class IncidentImageController {

    private final IncidentImageService imageService;

    /** GET /v1/incidents/{incidentId}/images */
    @GetMapping
    public ResponseEntity<ApiResponse<List<IncidentImageResponseDTO>>> getImages(
            @PathVariable @Positive Long incidentId) {

        return ResponseEntity.ok(ApiResponse.ok(imageService.getByIncidentId(incidentId)));
    }

    /**
     * Sube una imagen de evidencia a la incidencia.
     * El cliente Angular comprime antes de enviar (browser-image-compression).
     * POST /v1/incidents/{incidentId}/images   (multipart/form-data, campo "file")
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<IncidentImageResponseDTO>> upload(
            @PathVariable @Positive Long incidentId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) throws IOException {

        IncidentImageResponseDTO result =
                imageService.upload(incidentId, file, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    /** DELETE /v1/incidents/{incidentId}/images/{imageId} */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable @Positive Long incidentId,
            @PathVariable @Positive Long imageId) throws IOException {

        imageService.delete(incidentId, imageId);
        return ResponseEntity.ok(ApiResponse.ok("Imagen de evidencia eliminada correctamente."));
    }
}