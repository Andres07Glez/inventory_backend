package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.domains.Invoice;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.InvoiceRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.InvoicePdfService;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePdfServiceImpl implements InvoicePdfService {

    private static final long   MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20 MB
    private static final String ALLOWED_MIME  = "application/pdf";

    private final InvoiceRepository invoiceRepository;
    private final UserRepository    userRepository;
    private final StorageService    storageService;

    @Override
    @Transactional
    public String uploadPdf(Long invoiceId, MultipartFile file, Long uploadedById) {

        // ── 1. Validar aquí, ANTES de llamar al StorageService ───────────────
        //    El StorageService no necesita saber nada de PDFs.
        //    Cada servicio de negocio es responsable de su propia validación.
        validatePdf(file);

        // ── 2. Verificar que el usuario exista ────────────────────────────────
        userRepository.findByIdAndIsActiveTrue(uploadedById)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado o inactivo: " + uploadedById));

        // ── 3. Obtener la factura ─────────────────────────────────────────────
        Invoice invoice = findOrThrow(invoiceId);

        // ── 4. Si ya tenía un PDF previo, eliminar el físico ──────────────────
        //    Solo se intenta borrar si la ruta tiene el formato esperado (invoices/...),
        //    para no fallar silenciosamente con rutas viejas o nombres sueltos en BD.
        String previousPath = invoice.getDocumentPath();
        if (previousPath != null && !previousPath.isBlank()) {
            if (previousPath.startsWith("invoices/")) {
                storageService.delete(previousPath);
                log.debug("PDF anterior eliminado para factura id={} ruta={}", invoiceId, previousPath);
            } else {
                log.warn("PDF anterior de factura id={} tiene ruta en formato no reconocido '{}', " +
                        "se omite el borrado físico pero se reemplaza en BD.", invoiceId, previousPath);
            }
        }

        // ── 5. Guardar el archivo físico ──────────────────────────────────────
        //    subDir = "invoices/{invoiceId}" → el StorageService crea la carpeta
        //    y genera el nombre: {uuid}.pdf
        //    Ruta final: uploads/invoices/{invoiceId}/{uuid}.pdf
        String subDir       = "invoices/" + invoiceId;
        String relativePath = storageService.store(file, subDir);

        // ── 6. Persistir la ruta en BD ────────────────────────────────────────
        invoice.setDocumentPath(relativePath);
        invoiceRepository.save(invoice);

        String publicUrl = storageService.buildPublicUrl(relativePath);
        log.info("PDF subido para factura id={} → {}", invoiceId, relativePath);
        return publicUrl;
    }

    @Override
    @Transactional
    public void deletePdf(Long invoiceId) {
        Invoice invoice = findOrThrow(invoiceId);

        if (invoice.getDocumentPath() == null || invoice.getDocumentPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "La factura no tiene un documento PDF asociado.");
        }

        storageService.delete(invoice.getDocumentPath());
        invoice.setDocumentPath(null);
        invoiceRepository.save(invoice);
        log.info("PDF eliminado para factura id={}", invoiceId);
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Factura no encontrada: " + id));
    }

    /**
     * Valida tipo y tamaño del PDF ANTES de enviarlo al StorageService.
     * El StorageService no sabe nada de esta validación — y no tiene por qué saberlo.
     */
    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo está vacío.");
        }
        if (!ALLOWED_MIME.equals(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Solo se aceptan archivos PDF (application/pdf).");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "El archivo supera el tamaño máximo permitido de 20 MB.");
        }
    }
}