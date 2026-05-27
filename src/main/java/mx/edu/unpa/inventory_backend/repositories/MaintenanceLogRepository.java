package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.MaintenanceLog;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MaintenanceLogRepository extends JpaRepository<MaintenanceLog, Long> {

    /**
     * Registros de mantenimiento de un bien específico, ordenados del más reciente al más antiguo.
     * JOIN FETCH para evitar N+1 al acceder a asset y createdBy.
     */
    @Query("""
            SELECT m FROM MaintenanceLog m
            JOIN FETCH m.asset a
            JOIN FETCH m.createdBy
            WHERE a.id = :assetId
            ORDER BY m.performedDate DESC, m.createdAt DESC
            """)
    List<MaintenanceLog> findByAssetIdOrdered(@Param("assetId") Long assetId);

    /**
     * Lista global con filtro opcional por tipo de mantenimiento.
     * Cuando {@code type} es null, devuelve todos los registros.
     */
    @Query("""
            SELECT m FROM MaintenanceLog m
            JOIN FETCH m.asset a
            JOIN FETCH m.createdBy
            WHERE (:type IS NULL OR m.maintenanceType = :type)
            ORDER BY m.performedDate DESC, m.createdAt DESC
            """)
    List<MaintenanceLog> findAllFiltered(@Param("type") MaintenanceType type);

    /**
     * Detalle completo con fetch de asset, createdBy e incident (LEFT JOIN porque es nullable).
     */
    @Query("""
            SELECT m FROM MaintenanceLog m
            JOIN FETCH m.asset
            JOIN FETCH m.createdBy
            LEFT JOIN FETCH m.incident
            WHERE m.id = :id
            """)
    Optional<MaintenanceLog> findByIdWithDetails(@Param("id") Long id);

    /**
     * Verifica si un incident ya está vinculado a otro mantenimiento del mismo bien.
     * Útil para validar que incidentId pertenece al mismo assetId.
     */
    @Query("""
            SELECT COUNT(m) > 0 FROM MaintenanceLog m
            WHERE m.incident.id = :incidentId
              AND m.asset.id != :assetId
            """)
    boolean existsIncidentLinkedToOtherAsset(
            @Param("incidentId") Long incidentId,
            @Param("assetId")    Long assetId
    );
}