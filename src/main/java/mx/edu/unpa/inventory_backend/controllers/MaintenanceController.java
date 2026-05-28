package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.maintenance.request.MaintenanceCreateRequest;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceResponse;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceSummary;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.MaintenanceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    // ── POST /v1/maintenance ────────────────────────────────────────────────────

    /**
     * Registra un nuevo mantenimiento.
     * ADMIN y OPERADOR pueden crear; AUDITOR solo lectura.
     *
     * Nota: el userId nunca viene en el body — se extrae del JWT via @AuthenticationPrincipal.
     */
    @PostMapping("/v1/maintenance")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> create(
            @Valid @RequestBody MaintenanceCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        MaintenanceResponse response = maintenanceService.create(request, currentUser.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    // ── GET /v1/maintenance ─────────────────────────────────────────────────────

    /**
     * Lista global con filtro opcional por tipo.
     * Accesible para todos los roles autenticados.
     *
     * @param type filtro opcional: PREVENTIVE | CORRECTIVE | WARRANTY
     */
    @GetMapping("/v1/maintenance")
    public ResponseEntity<ApiResponse<List<MaintenanceSummary>>> getAll(
            @RequestParam(required = false) MaintenanceType type) {

        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getAll(type)));
    }

    // ── GET /v1/maintenance/{id} ────────────────────────────────────────────────

    /**
     * Detalle completo de un registro.
     */
    @GetMapping("/v1/maintenance/{id}")
    public ResponseEntity<ApiResponse<MaintenanceResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getById(id)));
    }

    // ── GET /v1/assets/{assetId}/maintenance ───────────────────────────────────

    /**
     * Lista los mantenimientos de un bien específico.
     * Ruta RESTful de sub-recurso — no duplicar con filtro en /v1/maintenance.
     */
    @GetMapping("/v1/assets/{assetId}/maintenance")
    public ResponseEntity<ApiResponse<List<MaintenanceSummary>>> getByAsset(
            @PathVariable Long assetId) {

        return ResponseEntity.ok(ApiResponse.ok(maintenanceService.getByAssetId(assetId)));
    }

    // Agrega este endpoint al final de la clase
    /**
     * Elimina un registro de mantenimiento por su ID.
     * Solo accesible para el rol ADMIN.
     */
    @DeleteMapping("/v1/maintenance/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        maintenanceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}