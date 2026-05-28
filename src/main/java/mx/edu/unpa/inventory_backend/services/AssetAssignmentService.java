package mx.edu.unpa.inventory_backend.services;


import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetAssignmentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetAssignmentResponseDTO;

public interface AssetAssignmentService {
    AssetAssignmentResponseDTO assignAsset(AssetAssignmentRequestDTO request,Long assignedById);
}
