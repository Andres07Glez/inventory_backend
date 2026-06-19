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
import mx.edu.unpa.inventory_backend.dtos.asset_assignment.response.AssignmentHistoryResponse;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.AssetQueryService;
import mx.edu.unpa.inventory_backend.services.AssetService;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetSearchResultDTO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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

    @GetMapping("/search/typeahead") // <--- RUTA MODIFICADA PARA EVITAR CONFLICTO
    public ResponseEntity<ApiResponse<List<AssetSearchResultDTO>>> searchTypeahead(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        if (q == null || q.trim().length() < 2) {
            return ResponseEntity.ok(ApiResponse.ok(List.of()));
        }

        int maxLimit = 30; // Declarado aquí por simplicidad
        int safeLimit = Math.min(Math.max(limit, 1), maxLimit);

        List<AssetSearchResultDTO> results =
                assetRepository.searchActive(q.trim(), safeLimit);

        return ResponseEntity.ok(ApiResponse.ok(results));
    }

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
            @AuthenticationPrincipal AuthenticatedUser currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(assetService.registerAsset(request, currentUser.id())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AssetResumeResponse>>> getAllAssets(
            @RequestParam(required = false) ConditionStatus conditionStatus,
            @RequestParam(required = false) LifecycleStatus lifecycleStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {

        Page<AssetResumeResponse> result = assetService.getAllAssets(
                conditionStatus,
                lifecycleStatus,
                startDate,
                endDate,
                pageable
        );

        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/{id}/condition/")
    public ResponseEntity<ApiResponse<UpdateConditionResponse>> updateCondition(
            @PathVariable @Positive(message = "El ID debe ser un número positivo") Long id,
            @Valid @RequestBody UpdateConditionRequest request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(ApiResponse.ok(
                assetService.updateCondition(id, request, currentUser.id())));
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
