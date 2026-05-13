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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository  invoiceRepository;
    private final AssetRepository    assetRepository;
    private final UserRepository     userRepository;
    private final SupplierRepository supplierRepository;

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
        invoice.setDocumentPath(request.getDocumentPath());
        invoice.setNotes(request.getNotes());

        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado: " + request.getSupplierId()));
        invoice.setSupplier(supplier);
    }

    private InvoiceResponseDTO toDTO(Invoice i) {
        Supplier s = i.getSupplier();
        return new InvoiceResponseDTO(
                i.getId(),
                i.getInvoiceNumber(),
                s != null ? s.getId()   : null,
                s != null ? s.getName() : null,
                i.getInvoiceDate(),
                i.getTotalAmount(),
                i.getDocumentPath(),
                i.getNotes(),
                i.getCreatedAt(),
                i.getCreatedBy() != null ? i.getCreatedBy().getFullName() : null
        );
    }
}