package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {


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
}