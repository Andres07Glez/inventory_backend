package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.components.InventoryNumberGenerator;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.*;
import mx.edu.unpa.inventory_backend.services.AssetService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // 3. Validar ubicación activa (opcional al registrar)
        Location location = null;
        if (request.getLocationId() != null) {
            location = locationRepository.findByIdAndIsActiveTrue(request.getLocationId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Ubicación no encontrada o inactiva: " + request.getLocationId()));
        }

        // 3.5 Resolver factura (opcional)
        Invoice invoice = null;
        if (request.getInvoiceId() != null) {
            invoice = invoiceRepository.findById(request.getInvoiceId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Factura no encontrada: " + request.getInvoiceId()));
        }

        // 4. Validar unicidad de barcode
        if (request.getBarcode() != null && !request.getBarcode().isBlank()) {
            if (assetRepository.existsByBarcodeAndBarcodeIsNotNull(request.getBarcode().trim())) {
                throw new DuplicateResourceException(
                        "Ya existe un bien con el código de barras: " + request.getBarcode());
            }
        }

        // 5. Validar unicidad de número de serie
        if (request.getSerialNumber() != null && !request.getSerialNumber().isBlank()) {
            if (assetRepository.existsBySerialNumber(request.getSerialNumber().trim())) {
                throw new DuplicateResourceException(
                        "Ya existe un bien con el número de serie: " + request.getSerialNumber());
            }
        }

        // 6. Resolver condición física (default GOOD si no se envía)
        ConditionStatus condition = ConditionStatus.GOOD;
        if (request.getConditionStatus() != null && !request.getConditionStatus().isBlank()) {
            try {
                condition = ConditionStatus.valueOf(request.getConditionStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Condición inválida: " + request.getConditionStatus()
                                + ". Valores aceptados: GOOD, REGULAR, BAD");
            }
        }

        // 7. Construir la entidad Asset (inventoryNumber se asigna después del save)
        Asset asset = new Asset();
        asset.setDescription(request.getDescription().trim());
        asset.setBrand(request.getBrand());
        asset.setModel(request.getModel());
        asset.setSerialNumber(request.getSerialNumber() != null
                ? request.getSerialNumber().trim() : null);
        asset.setBarcode(request.getBarcode() != null
                ? request.getBarcode().trim() : null);
        asset.setNotes(request.getNotes());
        asset.setCategory(category);
        asset.setLocation(location);

        asset.setInvoice(invoice);
        asset.setInvoiceDate(request.getInvoiceDate() != null
                ? request.getInvoiceDate()
                : (invoice != null ? invoice.getInvoiceDate() : null));

        asset.setEntryDate(request.getEntryDate());
        asset.setConditionStatus(condition);
        asset.setCreatedBy(creator);
        asset.setUpdatedBy(creator);
        // inventoryNumber se deja null temporalmente — se asigna tras el primer save

        //asset.setInventoryNumber("PENDING");
        // ✅ Como debe quedar
        boolean hasCustomNumber = request.getInventoryNumber() != null
                && !request.getInventoryNumber().isBlank();
        asset.setInventoryNumber(hasCustomNumber ? request.getInventoryNumber().trim() : "PENDING");

        /* 8. Primer save — MariaDB asigna el id autoincremental
        Asset saved = assetRepository.save(asset);
        // 9. Generar y asignar el número de inventario con el id ya conocido
        /*String inventoryNumber = inventoryNumberGenerator.generate(saved.getId());
        saved.setInventoryNumber(inventoryNumber);
        // 10. Segundo save — actualiza solo el inventoryNumber
        saved = assetRepository.save(saved);
        if ("PENDING".equals(saved.getInventoryNumber())) {
            saved.setInventoryNumber(inventoryNumberGenerator.generate(saved.getId()));
            saved = assetRepository.save(saved);
        }*/
        // 8. Primer save — MariaDB asigna el id
        Asset saved = assetRepository.save(asset);

        // 9. Flush para obtener el ID real sin cerrar la transacción
        entityManager.flush();

        // 10. Generar número de inventario y actualizar solo ese campo
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

        // 11. Mapear a DTO de respuesta
        return toResponseDTO(saved);
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
        dto.setBrand(asset.getBrand());
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
        dto.setCreatedByName(asset.getCreatedBy().getFullName());
        dto.setImageUrls(null); // imágenes se gestionan en endpoint separado
        return dto;
    }
}