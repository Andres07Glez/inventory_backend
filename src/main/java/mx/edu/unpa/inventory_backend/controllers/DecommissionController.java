package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.decommission.request.DecommissionRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.DecommissionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@RestController
@RequiredArgsConstructor
@Validated
public class DecommissionController {

    private final DecommissionService decommissionService;

    // ── Listado ───────────────────────────────────────────────────────────────


    @GetMapping("/v1/decommissions")
    public ResponseEntity<ApiResponse<Page<DecommissionSummaryDTO>>> list(
            @RequestParam(required = false) DecommissionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.ok(decommissionService.list(status, pageable)));
    }


    @GetMapping("/v1/decommissions/{id}")
    public ResponseEntity<ApiResponse<DecommissionResponseDTO>> getById(
            @PathVariable @Positive Long id) {

        return ResponseEntity.ok(ApiResponse.ok(decommissionService.getById(id)));
    }


    @GetMapping("/v1/assets/{assetId}/decommission")
    public ResponseEntity<ApiResponse<DecommissionResponseDTO>> getByAsset(
            @PathVariable @Positive Long assetId) {

        return ResponseEntity.ok(ApiResponse.ok(decommissionService.getByAssetId(assetId)));
    }

    // ── Crear baja (ADMIN o OPERADOR) ─────────────────────────────────────────

    @PostMapping(value = "/v1/decommissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DecommissionResponseDTO>> create(
            @RequestPart("request") @Valid DecommissionRequestDTO request,
            @RequestPart(value = "document", required = false) MultipartFile document,
            @AuthenticationPrincipal AuthenticatedUser currentUser) throws IOException {

        DecommissionResponseDTO created =
                decommissionService.create(request, document, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    // ── Confirmar baja definitiva (solo ADMIN) ────────────────────────────────


    @PatchMapping("/v1/decommissions/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DecommissionResponseDTO>> confirm(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                ApiResponse.ok(decommissionService.confirm(id, currentUser.id())));
    }
}
