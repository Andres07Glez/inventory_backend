package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.dtos.dashboard.response.LocationStatDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    boolean existsByBarcodeAndBarcodeIsNotNull(String barcode);

    boolean existsBySerialNumber(String serialNumber);

    boolean existsByInvoiceId(Long invoiceId);

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


    @Query("""
            SELECT a FROM Asset a
            JOIN FETCH a.category
            LEFT JOIN FETCH a.location
            WHERE a.id = :id
            """)
    Optional<Asset> findByIdWithDetails(@Param("id") Long id);

    long countByLifecycleStatusNot(LifecycleStatus lifecycleStatus);

    /** Bienes con un lifecycle específico (AVAILABLE, ASSIGNED, etc.) */
    long countByLifecycleStatus(LifecycleStatus lifecycleStatus);

    /**
     * Bienes de una condición dada que NO estén dados de baja.
     * Permite calcular las barras del donut chart sin contar bienes inactivos.
     */
    long countByConditionStatusAndLifecycleStatusNot(
            ConditionStatus conditionStatus,
            LifecycleStatus lifecycleStatus
    );

    // Top N ubicaciones con más bienes asignados actualmente.

    @Query("""
            SELECT new mx.edu.unpa.inventory_backend.dtos.dashboard.response.LocationStatDTO(
                l.name,
                l.campus,
                COUNT(a.id)
            )
            FROM Asset a
            JOIN a.location l
            WHERE a.lifecycleStatus = mx.edu.unpa.inventory_backend.enums.LifecycleStatus.ASSIGNED
            GROUP BY l.id, l.name, l.campus
            ORDER BY COUNT(a.id) DESC
            """)
    List<LocationStatDTO> findTopLocationsByAssignedAssets(Pageable pageable);
    @Query(value = """
    SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(inventory_number, '-', -1) AS UNSIGNED)), 0) + 1
    FROM assets
    WHERE inventory_number REGEXP '^INV-[0-9]{4}-[0-9]{5}$'
    AND SUBSTRING_INDEX(SUBSTRING_INDEX(inventory_number, '-', 2), '-', -1) = :year
    """, nativeQuery = true)
    Long getNextSequence(@Param("year") int year);
}