package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByBarcodeAndBarcodeIsNotNull(String barcode);

    boolean existsBySerialNumber(String serialNumber);

    @Query("""
            SELECT a FROM Asset a
            JOIN FETCH a.category
            LEFT JOIN FETCH a.location
            WHERE a.barcode = :barcode
              AND a.barcode IS NOT NULL
            """)
    Optional<Asset> findByBarcodeWithDetails(@Param("barcode") String barcode);

    /**
     * Busca por número de inventario institucional.
     * inventory_number siempre tiene valor (NOT NULL en DB), no necesita el guard extra.
     */
    @Query("""
            SELECT a FROM Asset a
            JOIN FETCH a.category
            LEFT JOIN FETCH a.location
            WHERE a.inventoryNumber = :inventoryNumber
            """)
    Optional<Asset> findByInventoryNumberWithDetails(@Param("inventoryNumber") String inventoryNumber);
    @EntityGraph(attributePaths = {"category", "location"})
    @Query("SELECT a FROM Asset a WHERE " +
            "(:condition IS NULL OR a.conditionStatus = :condition) AND " +
            "(:lifecycle IS NULL OR a.lifecycleStatus = :lifecycle)")
    Page<Asset> findByFilters(
            @Param("condition") ConditionStatus condition,
            @Param("lifecycle") LifecycleStatus lifecycle,
            Pageable pageable);

    @Query("SELECT COALESCE(MAX(a.id), 0) + 1 FROM Asset a")
    Long getNextId();

    @Query("""
            SELECT a FROM Asset a
            JOIN FETCH a.category
            LEFT JOIN FETCH a.location
            WHERE a.id = :id
            """)
    Optional<Asset> findByIdWithDetails(@Param("id") Long id);
}