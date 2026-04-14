package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetAssignment;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.AssetMapper;
import mx.edu.unpa.inventory_backend.repositories.AssetAssignmentRepository;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.services.AssetQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetQueryServiceImpl implements AssetQueryService {

    private final AssetRepository assetRepository;
    private final AssetAssignmentRepository assignmentRepository;
    private final AssetMapper assetMapper;

    @Override
    @Transactional(readOnly = true)
    public AssetDetailResponse findByCode(String code) {
        log.debug("Buscando bien con código: {}", code);

        String trimmedCode = code.trim();

        Asset asset = assetRepository.findByBarcodeWithDetails(trimmedCode)
                .or(() -> assetRepository.findByInventoryNumberWithDetails(trimmedCode))
                .orElseThrow(() -> {
                    log.warn("Bien no encontrado con código: {}", trimmedCode);
                    return new ResourceNotFoundException(
                            "No se encontró ningún bien con el código: " + trimmedCode
                    );
                });

        Optional<AssetAssignment> activeAssignment = assignmentRepository.findActiveByAssetId(asset.getId());

        log.debug("Bien encontrado: {} — asignación activa: {}", asset.getInventoryNumber(), activeAssignment.isPresent());

        return assetMapper.toDetailResponse(asset, activeAssignment.orElse(null));
    }
}
