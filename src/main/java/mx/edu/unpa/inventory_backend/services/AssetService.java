package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AssetService {
    Page<AssetResponseDTO> getAllAssets(ConditionStatus condition, LifecycleStatus lifecycle, Pageable pageable);
}
