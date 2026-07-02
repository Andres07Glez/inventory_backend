package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.request.UpdateConditionRequest;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.components.InventoryNumberGenerator;
import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResumeResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetSearchResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.UpdateConditionResponse;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.InvalidAssetStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.mappers.AssetCommandMapper;
import mx.edu.unpa.inventory_backend.mappers.AssetMapper;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.CategoryRepository;
import mx.edu.unpa.inventory_backend.repositories.LocationRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.repositories.*;
import mx.edu.unpa.inventory_backend.services.AssetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final CategoryRepository categoryRepository;
    private final LocationRepository locationRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final InventoryNumberGenerator inventoryNumberGenerator;
    private final AssetMapper assetMapper;
    private final AssetCommandMapper assetCommandMapper;
    private final BrandRepository brandRepository;

    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public AssetResponseDTO registerAsset(AssetRequestDTO request, Long userId) {

        // 1. Resolver usuario autenticado
        User creator = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado o inactivo: " + userId));

        // 2. Validar categoría activa
        Category category = categoryRepository.findByIdAndIsActiveTrue(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Categoría no encontrada o inactiva: " + request.getCategoryId()));

        // 3. Resolver entidades opcionales
        Location location = resolveLocation(request.getLocationId());
        Invoice invoice   = resolveInvoice(request.getInvoiceId());
        Brand brand       = resolveBrand(request.getBrandId());

        // 4. Validar unicidad de barcode y número de serie
        validateBarcode(request.getBarcode());
        validateSerialNumber(request.getSerialNumber());

        // 5. Resolver condición física (default GOOD si no se envía)
        ConditionStatus condition = resolveConditionStatus(request.getConditionStatus());

        // 6. Construir la entidad Asset (inventoryNumber se asigna después del save)
        Asset asset = new Asset();
        asset.setDescription(request.getDescription().trim());
        asset.setBrand(brand);
        asset.setModel(request.getModel());
        asset.setSerialNumber(request.getSerialNumber() != null
                ? request.getSerialNumber().trim() : null);
        asset.setBarcode(request.getBarcode() != null
                ? request.getBarcode().trim() : null);
        asset.setNotes(request.getNotes());
        asset.setCategory(category);
        asset.setLocation(location);
        asset.setInvoice(invoice);

        // Si viene la fecha en el request, se usa; si no, se intenta sacar de la factura; si no, queda null.
        asset.setInvoiceDate(Optional.ofNullable(request.getInvoiceDate())
                .orElse(invoice != null ? invoice.getInvoiceDate() : null));

        asset.setEntryDate(request.getEntryDate());
        asset.setConditionStatus(condition);
        asset.setCreatedBy(creator);
        asset.setUpdatedBy(creator);
        boolean hasCustomNumber = request.getInventoryNumber() != null
                && !request.getInventoryNumber().isBlank();
        asset.setInventoryNumber(hasCustomNumber ? request.getInventoryNumber().trim() : "PENDING");

        // 7. Primer save — MariaDB asigna el id
        Asset saved = assetRepository.save(asset);

        // 8. Flush para obtener el ID real sin cerrar la transacción
        entityManager.flush();

        // 9. Generar número de inventario y actualizar solo ese campo
        if ("PENDING".equals(saved.getInventoryNumber())) {
            String inventoryNumber = inventoryNumberGenerator.generate(saved.getId());
            saved.setInventoryNumber(inventoryNumber);
            // UPDATE de un solo campo sin disparar otro ciclo de AUTO_INCREMENT
            entityManager.createQuery(
                            "UPDATE Asset a SET a.inventoryNumber = :inv WHERE a.id = :id")
                    .setParameter("inv", inventoryNumber)
                    .setParameter("id", saved.getId())
                    .executeUpdate();
        }

        log.info("Bien registrado: {} por usuario id={}", saved.getInventoryNumber(), userId);

        // 10. Mapear a DTO de respuesta
        return toResponseDTO(saved);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<AssetResumeResponse> getAllAssets(
            ConditionStatus condition,
            LifecycleStatus lifecycle,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        return assetRepository.findFiltered(condition, lifecycle, startDate, endDate, pageable)
                .map(assetMapper::toDto);
    }

    // ---------------------------------------------------------------
    // Métodos auxiliares de resolución — reducen la complejidad cognitiva
    // de registerAsset al extraer cada bloque if a su propio método.
    // ---------------------------------------------------------------

    /** Resuelve la ubicación activa si se proporcionó un ID; de lo contrario retorna null. */
    private Location resolveLocation(Integer locationId) {
        if (locationId == null) return null;
        return locationRepository.findByIdAndIsActiveTrue(locationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ubicación no encontrada o inactiva: " + locationId));
    }

    /** Resuelve la factura si se proporcionó un ID; de lo contrario retorna null. */
    private Invoice resolveInvoice(Long invoiceId) {
        if (invoiceId == null) return null;
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Factura no encontrada: " + invoiceId));
    }

    /** Resuelve la marca activa si se proporcionó un ID; de lo contrario retorna null. */
    private Brand resolveBrand(Integer brandId) {
        if (brandId == null) return null;
        return brandRepository.findById(brandId)
                .filter(Brand::getIsActive)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Marca no encontrada o inactiva: " + brandId));
    }

    /** Valida que el código de barras no esté duplicado en el repositorio. */
    private void validateBarcode(String barcode) {
        if (barcode != null && !barcode.isBlank()
                && assetRepository.existsByBarcodeAndBarcodeIsNotNull(barcode.trim())) {
            throw new DuplicateResourceException(
                    "Ya existe un bien con el código de barras: " + barcode);
        }
    }

    /** Valida que el número de serie no esté duplicado en el repositorio. */
    private void validateSerialNumber(String serialNumber) {
        if (serialNumber != null && !serialNumber.isBlank()
                && assetRepository.existsBySerialNumber(serialNumber.trim())) {
            throw new DuplicateResourceException(
                    "Ya existe un bien con el número de serie: " + serialNumber);
        }
    }

    /**
     * Convierte el string de condición del request a su enum correspondiente.
     * Retorna {@link ConditionStatus#GOOD} si no se proporcionó valor.
     */
    private ConditionStatus resolveConditionStatus(String conditionStatus) {
        if (conditionStatus == null || conditionStatus.isBlank()) {
            return ConditionStatus.GOOD;
        }
        try {
            return ConditionStatus.valueOf(conditionStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Condición inválida: " + conditionStatus
                            + ". Valores aceptados: GOOD, REGULAR, BAD");
        }
    }

    // ---------------------------------------------------------------
    // Mapper manual — sin MapStruct porque AssetResponseDTO usa @Data
    // y mezcla campos de auditoría que no están en AssetDetailResponse
    // ---------------------------------------------------------------
    private AssetResponseDTO toResponseDTO(Asset asset) {
        AssetResponseDTO dto = new AssetResponseDTO();
        dto.setId(asset.getId());
        dto.setInventoryNumber(asset.getInventoryNumber());
        dto.setBarcode(asset.getBarcode());
        dto.setDescription(asset.getDescription());
        dto.setBrand(asset.getBrand() != null ? asset.getBrand().getName() : null);
        dto.setModel(asset.getModel());
        dto.setSerialNumber(asset.getSerialNumber());
        dto.setNotes(asset.getNotes());
        dto.setCategoryName(asset.getCategory().getName());
        dto.setLocationName(asset.getLocation() != null ? asset.getLocation().getName() : null);
        dto.setCampus(asset.getLocation() != null ? asset.getLocation().getCampus() : null);
        dto.setEntryDate(asset.getEntryDate());
        dto.setConditionStatus(asset.getConditionStatus().name());
        dto.setLifecycleStatus(asset.getLifecycleStatus().name());
        dto.setCreatedAt(asset.getCreatedAt());
        dto.setCreatedByName(asset.getCreatedBy().getGuardian().getFullName());
        dto.setImageUrls(null);
        return dto;
    }

    @Transactional
    @Override
    public UpdateConditionResponse updateCondition(Long assetId, UpdateConditionRequest request, Long updatedBy) {
        log.info("Actualizando condición del bien ID={} → {}", assetId, request.conditionStatus());

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el bien con ID: " + assetId
                ));
        User user = userRepository.findById(updatedBy)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró el usuario con ID: " + updatedBy
                ));

        // Edge case: un bien dado de baja es inmutable
        validateAssetIsModifiable(asset);

        // Capturar el estado anterior ANTES de modificar (para la respuesta)
        ConditionStatus previousCondition = asset.getConditionStatus();

        // Aplicar el cambio
        asset.setConditionStatus(request.conditionStatus());
        asset.setUpdatedBy(user);

        Asset savedAsset = assetRepository.save(asset);

        log.info("Condición actualizada: bien={} | {} → {}",
                savedAsset.getInventoryNumber(), previousCondition, request.conditionStatus());

        return assetCommandMapper.toUpdateConditionResponse(savedAsset, previousCondition);
    }

    /**
     * Valida que el bien se puede modificar dado su ciclo de vida actual.
     */
    private void validateAssetIsModifiable(Asset asset) {
        if (asset.getLifecycleStatus() == LifecycleStatus.DECOMMISSIONED) {
            throw new InvalidAssetStateException(
                    "No se puede modificar el bien '" + asset.getInventoryNumber() +
                            "' porque está dado de baja (DECOMMISSIONED)."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AssetSearchResponseDTO> searchAssets(String keyword, Pageable pageable) {
        return assetRepository.searchAssetsWithCurrentGuardian(keyword, pageable);
    }

    @Override
    @Transactional(readOnly = true)

    public Page<AssetResumeResponse> getMyAssignedAssets(Long guardianId, Pageable pageable) {
        return assetRepository.findAssignedToGuardian(guardianId, pageable)
                .map(assetMapper::toDto);
    }

}