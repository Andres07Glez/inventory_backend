package mx.edu.unpa.inventory_backend.controllers;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.services.InvoicePdfService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/invoices/{invoiceId}/pdf")
@RequiredArgsConstructor
public class InvoicePdfController {

    private final InvoicePdfService pdfService;

    /**
     * POST /v1/invoices/{invoiceId}/pdf
     * Sube (o reemplaza) el PDF de una factura.
     * Guarda en: uploads/invoices/{invoiceId}/{uuid}.pdf
     *
     * Body: multipart/form-data, campo "file"
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> upload(
            @PathVariable Long invoiceId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AuthenticatedUser currentUser) {

        String publicUrl = pdfService.uploadPdf(invoiceId, file, currentUser.id());
        return ResponseEntity.ok(ApiResponse.ok(publicUrl));
    }

    /**
     * DELETE /v1/invoices/{invoiceId}/pdf
     * Elimina el PDF de la factura (físico + limpia documentPath en BD).
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<String>> delete(
            @PathVariable Long invoiceId) {

        pdfService.deletePdf(invoiceId);
        return ResponseEntity.ok(ApiResponse.ok("Documento PDF eliminado correctamente."));
    }
}