package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.request.UpdateConditionRequest;
import mx.edu.unpa.inventory_backend.dtos.asset.response.*;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResumeResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.UpdateConditionResponse;
import mx.edu.unpa.inventory_backend.dtos.assetAssigment.response.AssignmentHistoryResponse;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.services.AssetQueryService;
import mx.edu.unpa.inventory_backend.services.AssetService;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/assets")
@RequiredArgsConstructor
@Validated
public class AssetController {

    private final AssetQueryService assetQueryService;
    private final AssetService assetService;
    private final AssetRepository assetRepository;

    @GetMapping("/lookup")
    public ResponseEntity<ApiResponse<AssetDetailResponse>> lookupByCode(
            @RequestParam
            @NotBlank(message = "El código no puede estar vacío")
            @Size(max = 100, message = "El código no puede exceder 100 caracteres")
            String q
    ) {
        AssetDetailResponse response = assetQueryService.findByCode(q);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<AssetSearchResponseDTO>> searchAssets(
            @RequestParam(required = false, defaultValue = "") String keyword,
            Pageable pageable) {

        Page<AssetSearchResponseDTO> result = assetService.searchAssets(keyword, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Búsqueda explícita por código de barras.
     * Útil cuando la UI tiene un campo dedicado para el escáner.
     */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<ApiResponse<AssetDetailResponse>> findByBarcode(
            @PathVariable
            @NotBlank(message = "El código de barras no puede estar vacío")
            @Size(max = 100, message = "El código de barras no puede exceder 100 caracteres")
            String barcode
    ) {
        AssetDetailResponse response = assetQueryService.findByCode(barcode);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Búsqueda explícita por número de inventario institucional.
     * Úsalo cuando el usuario escribe manualmente el número de inventario.
     */
    @GetMapping("/inventory-number/{inventoryNumber}")
    public ResponseEntity<ApiResponse<AssetDetailResponse>> findByInventoryNumber(
            @PathVariable
            @NotBlank(message = "El número de inventario no puede estar vacío")
            @Size(max = 30, message = "El número de inventario no puede exceder 30 caracteres")
            String inventoryNumber
    ) {
        AssetDetailResponse response = assetQueryService.findByCode(inventoryNumber);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AssetResponseDTO>> registerAsset(
            @Valid @RequestBody AssetRequestDTO request,
            @RequestParam Long userId  // temporal — se reemplaza por JWT en el módulo de seguridad
    ) {
        AssetResponseDTO response = assetService.registerAsset(request, userId);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<Page<AssetResumeResponse>> getAssets(
            @RequestParam(required = false) ConditionStatus conditionStatus,
            @RequestParam(required = false) LifecycleStatus lifecycleStatus,
            Pageable pageable) {

        Page<AssetResumeResponse> assets = assetService.getAllAssets(conditionStatus, lifecycleStatus, pageable);
        return ResponseEntity.ok(assets);
    }
    @PatchMapping("/{id}/condition/{userId}")
    public ResponseEntity<ApiResponse<UpdateConditionResponse>> updateCondition(
            @PathVariable
            @Positive(message = "El ID debe ser un número positivo")
            Long id,
            @PathVariable
            Long userId,
            @RequestBody
            @Valid
            UpdateConditionRequest request
    ) {
        UpdateConditionResponse response = assetService.updateCondition(id, request,userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/next-folio")
    public ResponseEntity<ApiResponse<String>> getNextFolio() {
        int year = java.time.Year.now().getValue();
        Long nextSeq = assetRepository.getNextSequence(year);
        String folio = String.format("INV-%d-%05d", year, nextSeq);
        return ResponseEntity.ok(ApiResponse.ok(folio));
    }
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AssetDetailResponse>> findById(
            @PathVariable @Positive(message = "El ID debe ser un número positivo") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assetQueryService.findById(id)));
    }
    @GetMapping("/{id}/assignments")
    public ResponseEntity<ApiResponse<List<AssignmentHistoryResponse>>> getAssignmentHistory(
            @PathVariable @Positive(message = "El ID debe ser un número positivo") Long id) {
        return ResponseEntity.ok(ApiResponse.ok(assetQueryService.findAssignmentHistory(id)));
    }

    @PatchMapping("/{id}/condition")
    public ResponseEntity<ApiResponse<UpdateConditionResponse>> updateCondition(
            @PathVariable @Positive(message = "El ID debe ser un número positivo") Long id,
            @RequestBody @Valid UpdateConditionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(assetService.updateCondition(id, request,1L)));// 1L sustituir por usuarioId
    }
}
