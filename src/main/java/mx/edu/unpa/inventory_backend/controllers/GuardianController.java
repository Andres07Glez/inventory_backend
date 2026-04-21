package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.guardian.request.GuardianRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianResponseDTO;
import mx.edu.unpa.inventory_backend.services.GuardianService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/guardians")
@RequiredArgsConstructor
@Validated
public class GuardianController {

    private final GuardianService guardianService;

    // ── GET /api/v1/guardians ─────────────────────────────────────────────────
    /**
     * Lista los resguardantes activos con paginación.
     * Ejemplo: GET /api/v1/guardians?page=0&size=20&sort=fullName,asc
     */
    @GetMapping
    public ResponseEntity<Page<GuardianResponseDTO>> findAllActive(Pageable pageable) {
        return ResponseEntity.ok(guardianService.findAllActive(pageable));
    }

    // ── GET /api/v1/guardians/search?q=... ────────────────────────────────────
    /**
     * Búsqueda por nombre, número de empleado o departamento.
     * Ejemplo: GET /api/v1/guardians/search?q=karen&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<Page<GuardianResponseDTO>> search(
            @RequestParam
            @NotBlank(message = "El término de búsqueda no puede estar vacío")
            @Size(max = 100)
            String q,
            Pageable pageable
    ) {
        return ResponseEntity.ok(guardianService.search(q, pageable));
    }

    // ── GET /api/v1/guardians/{id} ────────────────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GuardianResponseDTO>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(guardianService.findById(id)));
    }

    // ── POST /api/v1/guardians ────────────────────────────────────────────────
    @PostMapping
    public ResponseEntity<ApiResponse<GuardianResponseDTO>> create(
            @Valid @RequestBody GuardianRequestDTO request
    ) {
        GuardianResponseDTO created = guardianService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created));
    }

    // ── PUT /api/v1/guardians/{id} ────────────────────────────────────────────
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GuardianResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody GuardianRequestDTO request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(guardianService.update(id, request)));
    }

    // ── DELETE /api/v1/guardians/{id} (baja lógica) ───────────────────────────
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        guardianService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
