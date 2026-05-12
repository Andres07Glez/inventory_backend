package mx.edu.unpa.inventory_backend.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.InvoiceService;
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

    // ── Listar todas las facturas
    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getAll()));
    }

    // ── Obtener factura por ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> getById(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.getById(id)));
    }

    // ── Crear factura
    @PostMapping
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> create(
            @Valid @RequestBody InvoiceRequestDTO request,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(invoiceService.create(request, currentUser.id())));
    }

    // ── Actualizar factura
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponseDTO>> update(
            @PathVariable Long id,
            @Valid @RequestBody InvoiceRequestDTO request) {
        return ResponseEntity.ok(ApiResponse.ok(invoiceService.update(id, request)));
    }

    // ── Eliminar factura (solo si no tiene bienes asociados)
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        invoiceService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
