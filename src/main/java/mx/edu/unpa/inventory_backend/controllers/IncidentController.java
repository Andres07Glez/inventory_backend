package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentCloseRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentStatusUpdateDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.IncidentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Endpoints disponibles:
 *   GET  /v1/incidents                     → listado paginado con filtros
 *   GET  /v1/assets/{assetId}/incidents    → incidencias de un bien
 *   GET  /v1/incidents/{id}               → detalle
 *   POST /v1/incidents                    → crear incidencia
 *   PATCH /v1/incidents/{id}/status       → avanzar estado (OPEN→IN_PROGRESS→RESOLVED)
 *   POST /v1/incidents/{id}/close         → cerrar (RESOLVED→CLOSED, tipo STANDARD)
 */
@RestController
@RequiredArgsConstructor
@Validated
public class IncidentController {

    private final IncidentService incidentService;

    // ── Consultas ─────────────────────────────────────────────────────────────

    /**
     * Listado global paginado con filtros opcionales.
     * GET /v1/incidents?status=OPEN&assetId=5&page=0&size=20
     */
    @GetMapping("/v1/incidents")
    public ResponseEntity<ApiResponse<Page<IncidentSummaryDTO>>> list(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) Long assetId,
            @RequestParam(required = false) String folio,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.ok(incidentService.list(status, assetId, folio, pageable)));
    }

    /**
     * Incidencias de un bien concreto — sin paginar, para el tab en el detalle.
     * GET /v1/assets/{assetId}/incidents
     */
    @GetMapping("/v1/assets/{assetId}/incidents")
    public ResponseEntity<ApiResponse<List<IncidentSummaryDTO>>> listByAsset(
            @PathVariable @Positive Long assetId) {

        return ResponseEntity.ok(
                ApiResponse.ok(incidentService.listByAsset(assetId)));
    }

    /**
     * Detalle completo de una incidencia.
     * GET /v1/incidents/{id}
     */
    @GetMapping("/v1/incidents/{id}")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> getById(
            @PathVariable @Positive Long id) {

        return ResponseEntity.ok(ApiResponse.ok(incidentService.getById(id)));
    }

    // ── Crear ─────────────────────────────────────────────────────────────────

    /**
     * Abre una nueva incidencia.
     * El bien se selecciona previamente con GET /v1/assets/search.
     * POST /v1/incidents
     */
    @PostMapping("/v1/incidents")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> create(
            @Valid @RequestBody IncidentRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        IncidentResponseDTO created = incidentService.create(request, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    // ── Flujo de estados ──────────────────────────────────────────────────────

    /**
     * Avanza el estado de la incidencia:
     *   OPEN → IN_PROGRESS
     *   IN_PROGRESS → RESOLVED
     *
     * PATCH /v1/incidents/{id}/status
     */
    @PatchMapping("/v1/incidents/{id}/status")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody IncidentStatusUpdateDTO dto) {

        return ResponseEntity.ok(ApiResponse.ok(incidentService.updateStatus(id, dto)));
    }

    /**
     * Cierre STANDARD de la incidencia (RESOLVED → CLOSED).
     * Accesible para ADMIN y OPERADOR.
     *
     * Si esta resolución requirió dar de baja el bien, usar:
     *   POST /v1/decommissions (con incidentId opcional para vincular)
     *
     * POST /v1/incidents/{id}/close
     */
    @PostMapping("/v1/incidents/{id}/close")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> close(
            @PathVariable @Positive Long id,
            @Valid @RequestBody IncidentCloseRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                ApiResponse.ok(incidentService.close(id, dto, currentUser.id())));
    }
}

