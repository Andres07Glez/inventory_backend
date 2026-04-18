package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResumeResponse;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface AssetService {
    /**
     * Registra un nuevo bien en el inventario.
     * Genera automáticamente el número de inventario institucional.
     *
     * @param request  datos del bien enviados por el cliente
     * @param userId   ID del usuario autenticado (extraído del token JWT)
     * @return         DTO con los datos del bien recién registrado
     */
    AssetResponseDTO registerAsset(AssetRequestDTO request, Long userId);

    Page<AssetResumeResponse> getAllAssets(ConditionStatus condition, LifecycleStatus lifecycle, Pageable pageable);
}
