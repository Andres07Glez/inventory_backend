package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.asset_assignment.response.AssignmentHistoryResponse;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;

import java.util.List;

public interface AssetQueryService {

    AssetDetailResponse findByCode(String code);
    AssetDetailResponse findById(Long id, AuthenticatedUser requestingUser);
    List<AssignmentHistoryResponse> findAssignmentHistory(Long assetId);
}
