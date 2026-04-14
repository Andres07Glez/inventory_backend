package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.services.AssetQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/assets")
@RequiredArgsConstructor
@Validated  // Activa las validaciones de @RequestParam y @PathVariable
public class AssetController {

    private final AssetQueryService assetQueryService;

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
}
