package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.mappers.AssetMapper;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.services.AssetService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    private final AssetMapper assetMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AssetResponseDTO> getAllAssets(ConditionStatus condition, LifecycleStatus lifecycle, Pageable pageable) {
        return assetRepository.findByFilters(condition, lifecycle, pageable)
                .map(assetMapper::toDto); // Mapeamos cada entidad del Page a DTO automáticamente
    }
}
