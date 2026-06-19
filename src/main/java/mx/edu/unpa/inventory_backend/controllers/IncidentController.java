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


@RestController
@RequiredArgsConstructor
@Validated
public class IncidentController {

    private final IncidentService incidentService;

    // ── Consultas ─────────────────────────────────────────────────────────────


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


    @GetMapping("/v1/assets/{assetId}/incidents")
    public ResponseEntity<ApiResponse<List<IncidentSummaryDTO>>> listByAsset(
            @PathVariable @Positive Long assetId) {

        return ResponseEntity.ok(
                ApiResponse.ok(incidentService.listByAsset(assetId)));
    }


    @GetMapping("/v1/incidents/{id}")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> getById(
            @PathVariable @Positive Long id) {

        return ResponseEntity.ok(ApiResponse.ok(incidentService.getById(id)));
    }

    // ── Crear ─────────────────────────────────────────────────────────────────


    @PostMapping("/v1/incidents")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> create(
            @Valid @RequestBody IncidentRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        IncidentResponseDTO created = incidentService.create(request, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(created));
    }

    // ── Flujo de estados ──────────────────────────────────────────────────────


    @PatchMapping("/v1/incidents/{id}/status")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> updateStatus(
            @PathVariable @Positive Long id,
            @Valid @RequestBody IncidentStatusUpdateDTO dto) {

        return ResponseEntity.ok(ApiResponse.ok(incidentService.updateStatus(id, dto)));
    }


    @PostMapping("/v1/incidents/{id}/close")
    public ResponseEntity<ApiResponse<IncidentResponseDTO>> close(
            @PathVariable @Positive Long id,
            @Valid @RequestBody IncidentCloseRequestDTO dto,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        return ResponseEntity.ok(
                ApiResponse.ok(incidentService.close(id, dto, currentUser.id())));
    }
}

