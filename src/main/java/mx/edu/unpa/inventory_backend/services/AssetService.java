package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.request.UpdateConditionRequest;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResumeResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetSearchResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.UpdateConditionResponse;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

public interface AssetService {

    AssetResponseDTO registerAsset(AssetRequestDTO request, Long userId);

    Page<AssetResumeResponse> getAllAssets(ConditionStatus condition, LifecycleStatus lifecycle, LocalDate startDate,
                                           LocalDate endDate, Pageable pageable);

    @Transactional
    UpdateConditionResponse updateCondition(Long assetId, UpdateConditionRequest request, Long updatedBy);

    Page<AssetSearchResponseDTO> searchAssets(String keyword, Pageable pageable);
    Page<AssetResumeResponse> getMyAssignedAssets(Long guardianId, Pageable pageable);

}
