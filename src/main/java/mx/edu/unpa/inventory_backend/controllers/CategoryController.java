package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.category.request.CategoryRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.category.response.CategoryResponseDTO;
import mx.edu.unpa.inventory_backend.services.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/categories")
@RequiredArgsConstructor
@Validated
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Lista las categorías activas con paginación.
     */
    @GetMapping
    public ResponseEntity<Page<CategoryResponseDTO>> findAllActive(Pageable pageable) {
        return ResponseEntity.ok(categoryService.findAllActive(pageable));
    }

    /**
     * Búsqueda por nombre (solo activas).
     */
    @GetMapping("/search")
    public ResponseEntity<Page<CategoryResponseDTO>> search(
            @RequestParam
            @NotBlank(message = "El término de búsqueda no puede estar vacío")
            @Size(max = 100)
            String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(categoryService.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> create(
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> update(
            @PathVariable Integer id,
            @Valid @RequestBody CategoryRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(id, request)));
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Integer id) {
        categoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
