package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.location.request.LocationRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.location.response.LocationResponseDTO;
import mx.edu.unpa.inventory_backend.services.LocationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/locations")
@RequiredArgsConstructor
@Validated
public class LocationController {

    private final LocationService locationService;

    /**
     * Lista las ubicaciones activas con paginación.
     */
    @GetMapping
    public ResponseEntity<Page<LocationResponseDTO>> findAllActive(Pageable pageable) {
        return ResponseEntity.ok(locationService.findAllActive(pageable));
    }

    /**
     * Búsqueda por nombre, edificio o campus.
     */
    @GetMapping("/search")
    public ResponseEntity<Page<LocationResponseDTO>> search(
            @RequestParam
            @NotBlank(message = "El término de búsqueda no puede estar vacío")
            @Size(max = 100)
            String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(locationService.search(q, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponseDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<LocationResponseDTO>> create(
            @Valid @RequestBody LocationRequestDTO request
    ) {
        LocationResponseDTO created = locationService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<LocationResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody LocationRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(locationService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        locationService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
