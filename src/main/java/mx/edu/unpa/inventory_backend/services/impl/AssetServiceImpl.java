package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.request.UpdateConditionRequest;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.components.InventoryNumberGenerator;
import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResumeResponse;
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
import mx.edu.unpa.inventory_backend.services.AssetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository        assetRepository;
    private final CategoryRepository     categoryRepository;
    private final LocationRepository     locationRepository;
    private final UserRepository         userRepository;
    private final InventoryNumberGenerator inventoryNumberGenerator;
    private final AssetMapper assetMapper;
    private final AssetCommandMapper assetCommandMapper;


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
        asset.setEntryDate(request.getEntryDate());
        asset.setConditionStatus(condition);
        asset.setCreatedBy(creator);
        asset.setUpdatedBy(creator);
        // inventoryNumber se deja null temporalmente — se asigna tras el primer save
        asset.setInventoryNumber("PENDING");

        // 8. Primer save — MariaDB asigna el id autoincremental
        Asset saved = assetRepository.save(asset);

        // 9. Generar y asignar el número de inventario con el id ya conocido
        String inventoryNumber = inventoryNumberGenerator.generate(saved.getId());
        saved.setInventoryNumber(inventoryNumber);

        // 10. Segundo save — actualiza solo el inventoryNumber
        saved = assetRepository.save(saved);

        log.info("Bien registrado: {} por usuario id={}", saved.getInventoryNumber(), userId);

        // 11. Mapear a DTO de respuesta
        return toResponseDTO(saved);
    }
    @Transactional(readOnly = true)
    @Override
    public Page<AssetResumeResponse> getAllAssets(ConditionStatus condition, LifecycleStatus lifecycle, Pageable pageable) {
        return assetRepository.findByFilters(condition, lifecycle, pageable)
                .map(assetMapper::toDto); // Mapeamos cada entidad del Page a DTO automáticamente
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
                        "No se encontró el usuario con ID: " + assetId
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
     * */
    private void validateAssetIsModifiable(Asset asset) {
        if (asset.getLifecycleStatus() == LifecycleStatus.DECOMMISSIONED) {
            throw new InvalidAssetStateException(
                    "No se puede modificar el bien '" + asset.getInventoryNumber() +
                            "' porque está dado de baja (DECOMMISSIONED)."
            );
        }
    }

}
