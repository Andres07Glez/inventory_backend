package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /*@Query("SELECT COALESCE(MAX(a.id), 0) + 1 FROM Asset a")
    Long getNextId();*/
    /*@Query(value = "SELECT AUTO_INCREMENT FROM information_schema.TABLES " +
            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'assets'",
            nativeQuery = true)
    Long getNextAutoIncrement();*/
    @Query(value = """
    SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(inventory_number, '-', -1) AS UNSIGNED)), 0) + 1
    FROM assets
    WHERE inventory_number REGEXP '^INV-[0-9]{4}-[0-9]{5}$'
    AND SUBSTRING_INDEX(SUBSTRING_INDEX(inventory_number, '-', 2), '-', -1) = :year
    """, nativeQuery = true)
    Long getNextSequence(@Param("year") int year);
}