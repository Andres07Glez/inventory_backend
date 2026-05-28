package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.supplier.request.SupplierRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.supplier.response.SupplierResponseDTO;
import mx.edu.unpa.inventory_backend.services.SupplierService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/suppliers")
@RequiredArgsConstructor
@Validated
public class SupplierController {

    private final SupplierService supplierService;

    /**
     * Lista los proveedores activos con paginación.
     */
    @GetMapping
    public ResponseEntity<Page<SupplierResponseDTO>> findAllActive(Pageable pageable) {
        return ResponseEntity.ok(supplierService.findAllActive(pageable));
    }

    /**
     * Búsqueda por nombre o nombre de contacto.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<SupplierResponseDTO>> search(
            @RequestParam
            @NotBlank(message = "El término de búsqueda no puede estar vacío")
            @Size(max = 100)
            String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(supplierService.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> create(
            @Valid @RequestBody SupplierRequestDTO request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(supplierService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SupplierResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(supplierService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        supplierService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
