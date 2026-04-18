package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.services.AssetQueryService;
import mx.edu.unpa.inventory_backend.services.AssetService;
import org.springframework.http.HttpStatus;
import mx.edu.unpa.inventory_backend.services.AssetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/assets")
@RequiredArgsConstructor
@Validated  // Activa las validaciones de @RequestParam y @PathVariable
public class AssetController {

    private final AssetQueryService assetQueryService;
    private final AssetService assetService;

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

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<Page<AssetResponseDTO>> getAssets(
            @RequestParam(required = false) ConditionStatus conditionStatus,
            @RequestParam(required = false) LifecycleStatus lifecycleStatus,
            Pageable pageable) {

        Page<AssetResponseDTO> assets = assetService.getAllAssets(conditionStatus, lifecycleStatus, pageable);
        return ResponseEntity.ok(assets);
    }
}
