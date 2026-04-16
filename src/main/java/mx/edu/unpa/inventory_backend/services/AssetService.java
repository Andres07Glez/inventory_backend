package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;

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
}
