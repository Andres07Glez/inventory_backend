package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.image.response.AssetImageResponseDTO;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.AssetImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/v1/assets/{assetId}/images")
@RequiredArgsConstructor
public class AssetImageController {

    private final AssetImageService imageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AssetImageResponseDTO>>> getImages(
            @PathVariable @Positive Long assetId) {
        return ResponseEntity.ok(ApiResponse.ok(imageService.getByAssetId(assetId)));
    }

    /**
     * Sube una imagen al bien. Acepta multipart/form-data con el campo "file".
     * El cliente Angular debe comprimir antes de enviar (browser-image-compression).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AssetImageResponseDTO>> upload(
            @PathVariable @Positive Long assetId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) throws IOException {
        AssetImageResponseDTO result = imageService.upload(assetId, file, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable @Positive Long assetId,
            @PathVariable @Positive Long imageId) throws IOException {
        imageService.delete(assetId, imageId);
        return ResponseEntity.ok(ApiResponse.ok( "Imagen eliminada correctamente"));
    }

    @PatchMapping("/{imageId}/primary")
    public ResponseEntity<ApiResponse<AssetImageResponseDTO>> setPrimary(
            @PathVariable @Positive Long assetId,
            @PathVariable @Positive Long imageId) {
        return ResponseEntity.ok(ApiResponse.ok(imageService.setPrimary(assetId, imageId)));
    }
}
