package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.brand.request.BrandRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.brand.response.BrandResponseDTO;
import mx.edu.unpa.inventory_backend.services.BrandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    // Lista de marcas activas — para el selector en asset-registration
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(brandService.getAllActive()));
    }

    // Crear marca
    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponseDTO>> create(
            @Valid @RequestBody BrandRequestDTO request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(brandService.create(request)));
    }

    // Actualizar marca
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponseDTO>> update(
            @PathVariable Integer id,
            @Valid @RequestBody BrandRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(brandService.update(id, request)));
    }

    // Eliminar marca (soft delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id) {
        brandService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
