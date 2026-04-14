package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;

public interface AssetQueryService {

    AssetDetailResponse findByCode(String code);
}
