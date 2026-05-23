package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetSearchResultDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Fragmento de búsqueda de bienes para el flujo de creación de incidencias.
 *
 * INSTRUCCIÓN DE INTEGRACIÓN:
 *   Añade los métodos {@code searchForIncident} y {@code findByIdForSearch}
 *   directamente al {@code AssetRepository} existente si prefieres un solo
 *   repositorio. Este archivo está separado para no sobreescribir código
 *   ya generado del sprint anterior.
 */
public interface AssetSearchRepository extends JpaRepository<Asset, Long> {

    /**
     * Búsqueda dinámica typeahead sobre número de inventario, descripción,
     * número de serie y código de barras.
     *
     * Excluye bienes DECOMMISSIONED porque no tiene sentido reportar
     * una incidencia sobre un bien dado de baja.
     *
     * Resultado limitado por {@code limit} para rendimiento del typeahead.
     */
    @Query("""
            SELECT new mx.edu.unpa.inventory_backend.dtos.asset.response.AssetSearchResultDTO(
                a.id,
                a.inventoryNumber,
                a.description,
                COALESCE(b.name, ''),
                COALESCE(a.model, ''),
                COALESCE(a.serialNumber, ''),
                a.conditionStatus,
                a.lifecycleStatus,
                c.name,
                COALESCE(l.name, '')
            )
            FROM Asset a
            LEFT JOIN a.brand b
            JOIN a.category c
            LEFT JOIN a.location l
            WHERE a.lifecycleStatus <> mx.edu.unpa.inventory_backend.enums.LifecycleStatus.DECOMMISSIONED
              AND (
                  LOWER(a.inventoryNumber) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.description)     LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.serialNumber)    LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(a.barcode)         LIKE LOWER(CONCAT('%', :q, '%'))
              )
            ORDER BY
                CASE WHEN LOWER(a.inventoryNumber) LIKE LOWER(CONCAT(:q, '%')) THEN 0 ELSE 1 END,
                a.inventoryNumber ASC
            LIMIT :limit
            """)
    List<AssetSearchResultDTO> searchForIncident(
            @Param("q") String q,
            @Param("limit") int limit);
}
