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

/**
 * Módulo de Bajas de Bienes Patrimoniales.
 *
 * Completamente independiente del módulo de Incidencias.
 * Un bien puede darse de baja con o sin incidencia previa.
 *
 * Flujo:
 *   1. POST /v1/decommissions           → crea baja en estado PENDING (ADMIN o OPERADOR)
 *   2. PATCH /v1/decommissions/{id}/confirm → confirma baja → bien pasa a DECOMMISSIONED (solo ADMIN)
 *
 * Consultas:
 *   GET /v1/decommissions               → listado paginado (con filtro ?status=)
 *   GET /v1/decommissions/{id}          → detalle de una baja
 *   GET /v1/assets/{assetId}/decommission → baja del bien (si existe)
 */
@RestController
@RequiredArgsConstructor
@Validated
public class DecommissionController {

    private final DecommissionService decommissionService;

    // ── Listado ───────────────────────────────────────────────────────────────

    /**
     * GET /v1/decommissions?status=PENDING&page=0&size=20
     */
    @GetMapping("/v1/decommissions")
    public ResponseEntity<ApiResponse<Page<DecommissionSummaryDTO>>> list(
            @RequestParam(required = false) DecommissionStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.ok(decommissionService.list(status, pageable)));
    }

    /**
     * Detalle completo de una baja.
     * GET /v1/decommissions/{id}
     */
    @GetMapping("/v1/decommissions/{id}")
    public ResponseEntity<ApiResponse<DecommissionResponseDTO>> getById(
            @PathVariable @Positive Long id) {

        return ResponseEntity.ok(ApiResponse.ok(decommissionService.getById(id)));
    }

    /**
     * Baja de un bien específico (si existe).
     * GET /v1/assets/{assetId}/decommission
     *
     * Útil para mostrar el panel de baja en el detalle del bien en el frontend.
     * Retorna 404 si el bien no tiene ninguna baja registrada.
     */
    @GetMapping("/v1/assets/{assetId}/decommission")
    public ResponseEntity<ApiResponse<DecommissionResponseDTO>> getByAsset(
            @PathVariable @Positive Long assetId) {

        return ResponseEntity.ok(ApiResponse.ok(decommissionService.getByAssetId(assetId)));
    }

    // ── Crear baja (ADMIN o OPERADOR) ─────────────────────────────────────────

    /**
     * Inicia el proceso de baja de un bien (estado PENDING).
     * El bien se selecciona previamente con GET /v1/assets/search.
     * La incidencia de origen es OPCIONAL.
     *
     * POST /v1/decommissions   (multipart/form-data)
     *   - request  : JSON con los datos de la baja (@RequestPart)
     *   - document : PDF del acta administrativa (OPCIONAL)
     */
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

    /**
     * Confirma la baja definitiva del bien (PENDING → CONFIRMED).
     * El bien pasa a lifecycle_status = DECOMMISSIONED de forma atómica.
     *
     * Requiere rol ADMIN. OPERADOR recibe 403.
     *
     * PATCH /v1/decommissions/{id}/confirm
     */
    @PatchMapping("/v1/decommissions/{id}/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DecommissionResponseDTO>> confirm(
            @PathVariable @Positive Long id,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                ApiResponse.ok(decommissionService.confirm(id, currentUser.id())));
    }
}
