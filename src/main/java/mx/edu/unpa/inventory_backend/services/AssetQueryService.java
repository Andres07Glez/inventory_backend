package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.assetAssigment.response.AssignmentHistoryResponse;

import java.util.List;

public interface AssetQueryService {

    AssetDetailResponse findByCode(String code);
    AssetDetailResponse findById(Long id);
    List<AssignmentHistoryResponse> findAssignmentHistory(Long assetId);
}
