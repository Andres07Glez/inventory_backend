package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.AssetDecommission;
import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DecommissionRepository extends JpaRepository<AssetDecommission, Long> {

    /**
     * Detalle completo de una baja con todas sus relaciones.
     * Evita N+1 al cargar asset, incident (opcional), createdBy y confirmedBy.
     */
    @Query("""
            SELECT d FROM AssetDecommission d
            JOIN FETCH d.asset
            JOIN FETCH d.createdBy
            LEFT JOIN FETCH d.incident
            LEFT JOIN FETCH d.confirmedBy
            WHERE d.id = :id
            """)
    Optional<AssetDecommission> findByIdWithDetails(@Param("id") Long id);

    /**
     * Listado paginado con filtro opcional por estado.
     * Usado en GET /v1/decommissions.
     */
    @Query(value = """
            SELECT d FROM AssetDecommission d
            JOIN FETCH d.asset
            JOIN FETCH d.createdBy
            LEFT JOIN FETCH d.confirmedBy
            WHERE (:status IS NULL OR d.status = :status)
            ORDER BY d.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(d) FROM AssetDecommission d
            WHERE (:status IS NULL OR d.status = :status)
            """)
    Page<AssetDecommission> findAllFiltered(
            @Param("status") DecommissionStatus status,
            Pageable pageable);

    /**
     * Comprueba si ya existe una baja (en cualquier estado) para un bien dado.
     * Usado antes de crear una nueva baja para prevenir duplicados.
     *
     * Edge case: un bien solo puede tener una entrada en asset_decommissions
     * (UNIQUE KEY uq_decommissions_asset_id en DB). Esta consulta es la
     * validación en capa de servicio antes de que la DB lance una excepción.
     */
    boolean existsByAssetId(Long assetId);

    /**
     * Devuelve la baja de un bien si existe (independiente del estado).
     * Útil para mostrar el estado de baja en el detalle del bien.
     */
    @Query("""
            SELECT d FROM AssetDecommission d
            JOIN FETCH d.createdBy
            LEFT JOIN FETCH d.confirmedBy
            LEFT JOIN FETCH d.incident
            WHERE d.asset.id = :assetId
            """)
    Optional<AssetDecommission> findByAssetId(@Param("assetId") Long assetId);
}
