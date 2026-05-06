package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Supplier;
import mx.edu.unpa.inventory_backend.dtos.supplier.request.SupplierRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.supplier.response.SupplierResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.SupplierMapper;
import mx.edu.unpa.inventory_backend.repositories.SupplierRepository;
import mx.edu.unpa.inventory_backend.services.SupplierService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper     supplierMapper;

    // ── Crear ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SupplierResponseDTO create(SupplierRequestDTO request) {
        if (supplierRepository.existsByName(request.name())) {
            throw new DuplicateResourceException(
                    "Ya existe un proveedor con el nombre: " + request.name());
        }

        Supplier supplier = supplierMapper.toEntity(request);
        supplier.setIsActive(true);

        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    // ── Consultas ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> findAllActive(Pageable pageable) {
        return supplierRepository
                .findByIsActiveTrue(pageable)
                .map(supplierMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SupplierResponseDTO> search(String q, Pageable pageable) {
        return supplierRepository
                .searchActive(q, pageable)
                .map(supplierMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO findById(Long id) {
        return supplierMapper.toDto(getOrThrow(id));
    }

    // ── Actualizar ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public SupplierResponseDTO update(Long id, SupplierRequestDTO request) {
        Supplier supplier = getOrThrow(id);

        // Valida unicidad de nombre solo si cambió
        if (!supplier.getName().equalsIgnoreCase(request.name())
                && supplierRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new DuplicateResourceException(
                    "Ya existe un proveedor con el nombre: " + request.name());
        }

        supplierMapper.updateEntityFromDto(request, supplier);
        return supplierMapper.toDto(supplierRepository.save(supplier));
    }

    // ── Baja lógica ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deactivate(Long id) {
        Supplier supplier = getOrThrow(id);
        supplier.setIsActive(false);
        supplierRepository.save(supplier);
    }

    // ── Helper privado ────────────────────────────────────────────────────────

    private Supplier getOrThrow(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Proveedor no encontrado con id: " + id));
    }
}
