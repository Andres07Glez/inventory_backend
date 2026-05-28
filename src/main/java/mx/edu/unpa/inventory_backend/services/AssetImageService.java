package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.image.response.AssetImageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface AssetImageService {
     List<AssetImageResponseDTO> getByAssetId(Long assetId);
     AssetImageResponseDTO upload(Long assetId, MultipartFile file, Long uploadedById) throws IOException;
     void delete(Long assetId, Long imageId) throws IOException;
     AssetImageResponseDTO setPrimary(Long assetId, Long imageId);
 }
