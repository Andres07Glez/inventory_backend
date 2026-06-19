package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.domains.Invoice;
import mx.edu.unpa.inventory_backend.domains.Supplier;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.InvoiceRepository;
import mx.edu.unpa.inventory_backend.repositories.SupplierRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.InvoiceService;
import mx.edu.unpa.inventory_backend.storage.StorageService; // ← import nuevo
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private static final long   MAX_FILE_SIZE = 20 * 1024 * 1024L; // 20 MB
    private static final String ALLOWED_MIME  = "application/pdf";

    private final InvoiceRepository  invoiceRepository;
    private final AssetRepository    assetRepository;
    private final UserRepository     userRepository;
    private final SupplierRepository supplierRepository;
    private final StorageService     storageService;

    // ── Listar con paginación y búsqueda opcional ─────────────
    @Override
    public Page<InvoiceResponseDTO> getAll(String query, Pageable pageable) {
        boolean hasQuery = query != null && !query.isBlank();
        return hasQuery
                ? invoiceRepository.search(query.trim(), pageable).map(this::toDTO)
                : invoiceRepository.findAllByOrderByInvoiceDateDesc(pageable).map(this::toDTO);
    }

    // ── Listar todas sin paginar (para catálogos/selectores) ──
    @Override
    public List<InvoiceResponseDTO> getAllUnpaged() {
        return invoiceRepository.findAllByOrderByInvoiceDateDesc()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    // ── Obtener por ID ────────────────────────────────────────
    @Override
    public InvoiceResponseDTO getById(Long id) {
        return toDTO(findOrThrow(id));
    }

    // ── Crear ─────────────────────────────────────────────────
    @Override
    @Transactional
    public InvoiceResponseDTO create(InvoiceRequestDTO request, Long userId) {

        if (invoiceRepository.existsByInvoiceNumberIgnoreCase(request.getInvoiceNumber().trim())) {
            throw new DuplicateResourceException(
                    "Ya existe una factura con el número: " + request.getInvoiceNumber());
        }

        User creator = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado o inactivo: " + userId));

        Invoice invoice = new Invoice();
        invoice.setCreatedBy(creator);
        mapRequestToEntity(request, invoice);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Factura creada: {} por usuario id={}", saved.getInvoiceNumber(), userId);
        return toDTO(saved);
    }

    // ── Actualizar ────────────────────────────────────────────
    @Override
    @Transactional
    public InvoiceResponseDTO update(Long id, InvoiceRequestDTO request) {

        Invoice invoice = findOrThrow(id);
        String newNumber = request.getInvoiceNumber().trim();

        if (!invoice.getInvoiceNumber().equalsIgnoreCase(newNumber)
                && invoiceRepository.existsByInvoiceNumberIgnoreCase(newNumber)) {
            throw new DuplicateResourceException(
                    "Ya existe una factura con el número: " + newNumber);
        }

        mapRequestToEntity(request, invoice);
        Invoice saved = invoiceRepository.save(invoice);
        log.info("Factura actualizada id={} → {}", id, saved.getInvoiceNumber());
        return toDTO(saved);
    }

    // ── Eliminar ──────────────────────────────────────────────
    @Override
    @Transactional
    public void delete(Long id) {
        Invoice invoice = findOrThrow(id);

        if (assetRepository.existsByInvoiceId(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar la factura porque tiene bienes asociados.");
        }

        invoiceRepository.delete(invoice);
        log.info("Factura eliminada id={}", id);
    }

    // ── Subir PDF ─────────────────────────────────────────────
    @Override
    @Transactional
    public String uploadPdf(Long invoiceId, MultipartFile file, Long uploadedById) {

        // 1. Validar el archivo antes de cualquier operación de almacenamiento
        validatePdf(file);

        // 2. Verificar que el usuario exista y esté activo
        if (!userRepository.existsByIdAndIsActiveTrue(uploadedById)) {
            throw new ResourceNotFoundException(
                    "Usuario no encontrado o inactivo: " + uploadedById);
        }
        // 3. Obtener la factura
        Invoice invoice = findOrThrow(invoiceId);

        // 4. Si ya tenía un PDF previo, intentar eliminar el físico.
        //    StorageService.delete() es idempotente: no lanza si el archivo
        //    ya no existe, por lo que es seguro llamarlo con cualquier ruta
        //    (incluidas las antiguas que solo guardaban el nombre del archivo).
        String previousPath = invoice.getDocumentPath();
        if (previousPath != null && !previousPath.isBlank()) {
            storageService.delete(previousPath);
            log.debug("PDF anterior procesado para factura id={} ruta={}", invoiceId, previousPath);
        }

        String subDir       = "invoices/" + invoiceId;
        String relativePath = storageService.store(file, subDir);

        // 6. Persistir la ruta en BD
        invoice.setDocumentPath(relativePath);
        invoiceRepository.save(invoice);

        String publicUrl = storageService.buildPublicUrl(relativePath);
        log.info("PDF subido para factura id={} → {}", invoiceId, relativePath);
        return publicUrl;
    }

    // ── Eliminar PDF ──────────────────────────────────────────
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

    // ── Helpers privados ──────────────────────────────────────

    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Factura no encontrada: " + id));
    }

    private void mapRequestToEntity(InvoiceRequestDTO request, Invoice invoice) {
        invoice.setInvoiceNumber(request.getInvoiceNumber().trim());
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setTotalAmount(BigDecimal.ZERO);
        // documentPath NO se toca aquí: es gestionado exclusivamente por uploadPdf/deletePdf
        invoice.setNotes(request.getNotes());

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado: " + request.getSupplierId()));
        invoice.setSupplier(supplier);
    }

    private InvoiceResponseDTO toDTO(Invoice i) {
        Supplier s = i.getSupplier();

        String docUrl = (i.getDocumentPath() != null && !i.getDocumentPath().isBlank())
                ? storageService.buildPublicUrl(i.getDocumentPath())
                : null;

        return new InvoiceResponseDTO(
                i.getId(),
                i.getInvoiceNumber(),
                s != null ? s.getId()   : null,
                s != null ? s.getName() : null,
                i.getInvoiceDate(),
                i.getTotalAmount(),
                i.getDocumentPath(),
                docUrl,
                i.getNotes(),
                i.getCreatedAt(),
                i.getCreatedBy() != null ? i.getCreatedBy().getGuardian().getFullName() : null
        );
    }

    /**
     * Valida tipo MIME y tamaño del PDF antes de enviarlo al StorageService.
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