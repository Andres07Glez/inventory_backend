package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.InvoiceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    // ── Listar facturas paginadas con búsqueda opcional
    // Ejemplos:
    //   GET /v1/invoices?page=0&size=10
    //   GET /v1/invoices?q=Dell&page=0&size=10
    //   GET /v1/invoices?q=FAC-2024&page=0&size=10
    //   GET /v1/invoices?q=3&page=0&size=10   (busca por ID)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InvoiceResponseDTO>>> getAll(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 10, sort = "invoiceDate", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getAll(q, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> create(
            @Valid @RequestBody InvoiceRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.create(request, currentUser.id())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
