package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.AssetImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetImageRepository extends JpaRepository<AssetImage, Long> {

    List<AssetImage> findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(Long assetId);

    long countByAssetId(Long assetId);

    Optional<AssetImage> findByIdAndAssetId(Long imageId, Long assetId);

    Optional<AssetImage> findFirstByAssetIdAndIsPrimaryTrue(Long assetId);

    /** Desmarca todas las imágenes primarias de un bien antes de asignar una nueva */
    @Modifying
    @Query("UPDATE AssetImage ai SET ai.isPrimary = false WHERE ai.asset.id = :assetId")
    void clearPrimaryByAssetId(@Param("assetId") Long assetId);
}
