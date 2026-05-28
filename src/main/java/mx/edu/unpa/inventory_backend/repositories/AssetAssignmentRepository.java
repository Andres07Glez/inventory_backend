package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.AssetAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetAssignmentRepository extends JpaRepository<AssetAssignment, Long> {


    @Query("""
            SELECT aa FROM AssetAssignment aa
            JOIN FETCH aa.guardian
            WHERE aa.asset.id = :assetId
              AND aa.returnedAt IS NULL
            """)
    Optional<AssetAssignment> findActiveByAssetId(@Param("assetId") Long assetId);

    @Query("""
            SELECT aa FROM AssetAssignment aa
            JOIN FETCH aa.guardian
            LEFT JOIN FETCH aa.location
            LEFT JOIN FETCH aa.assignedBy
            WHERE aa.asset.id = :assetId
            ORDER BY aa.returnedAt NULLS FIRST, aa.assignedAt DESC
            """)
    List<AssetAssignment> findAllByAssetIdOrderByActivity(@Param("assetId") Long assetId);
}
