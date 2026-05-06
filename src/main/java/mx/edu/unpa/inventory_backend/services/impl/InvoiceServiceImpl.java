package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.domains.Invoice;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.InvoiceRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.InvoiceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;

    // ── Listar todas ──────────────────────────────────────────
    @Override
    public List<InvoiceResponseDTO> getAll() {
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

        // 1. Validar número de factura único
        if (invoiceRepository.existsByInvoiceNumberIgnoreCase(request.getInvoiceNumber().trim())) {
            throw new DuplicateResourceException(
                    "Ya existe una factura con el número: " + request.getInvoiceNumber());
        }

        // 2. Resolver usuario creador
        User creator = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado o inactivo: " + userId));

        // 3. Construir entidad y persistir
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

        // Valida duplicado solo si el número cambió
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

        // Verifica que no tenga bienes asociados antes de eliminar
        if (assetRepository.existsByInvoiceId(id)) {
            throw new IllegalStateException(
                    "No se puede eliminar la factura porque tiene bienes asociados.");
        }

        invoiceRepository.delete(invoice);
        log.info("Factura eliminada id={}", id);
    }

    // ── Helpers ───────────────────────────────────────────────
    private Invoice findOrThrow(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Factura no encontrada: " + id));
    }

    private void mapRequestToEntity(InvoiceRequestDTO request, Invoice invoice) {
        invoice.setInvoiceNumber(request.getInvoiceNumber().trim());
        invoice.setSupplier(request.getSupplier() != null
                ? request.getSupplier().trim() : null);
        invoice.setInvoiceDate(request.getInvoiceDate());
        invoice.setTotalAmount(request.getTotalAmount());
        invoice.setDocumentPath(request.getDocumentPath());
        invoice.setNotes(request.getNotes());
    }

    private InvoiceResponseDTO toDTO(Invoice i) {
        return new InvoiceResponseDTO(
                i.getId(),
                i.getInvoiceNumber(),
                i.getSupplier(),
                i.getInvoiceDate(),
                i.getTotalAmount(),
                i.getDocumentPath(),
                i.getNotes(),
                i.getCreatedAt(),
                i.getCreatedBy() != null ? i.getCreatedBy().getFullName() : null
        );
    }
}
