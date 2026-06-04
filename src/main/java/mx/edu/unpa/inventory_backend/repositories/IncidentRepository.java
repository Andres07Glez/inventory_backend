package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Incident;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    /**
     * Detalle completo de una incidencia con todas sus relaciones.
     * Usado en GET /v1/incidents/{id}.
     */
    @Query("""
            SELECT i FROM Incident i
            JOIN FETCH i.asset
            JOIN FETCH i.createdBy
            LEFT JOIN FETCH i.resolvedBy
            LEFT JOIN FETCH i.images img
            LEFT JOIN FETCH img.uploadedBy
            WHERE i.id = :id
            """)
    Optional<Incident> findByIdWithDetails(@Param("id") Long id);

    /**
     * Lista paginada de incidencias de un bien, ordenada de más reciente a más antigua.
     * Usada en la pestaña de incidencias del detalle del bien.
     */
    @Query(value = """
            SELECT i FROM Incident i
            JOIN FETCH i.asset
            JOIN FETCH i.createdBy
            LEFT JOIN FETCH i.resolvedBy
            WHERE i.asset.id = :assetId
            ORDER BY i.createdAt DESC
            """,
            countQuery = "SELECT COUNT(i) FROM Incident i WHERE i.asset.id = :assetId")
    Page<Incident> findByAssetId(@Param("assetId") Long assetId, Pageable pageable);

    /**
     * Listado global con filtros opcionales por estado.
     * Si status es null devuelve todos.
     */
    @Query(value = """
            SELECT i FROM Incident i
            JOIN FETCH i.asset
            JOIN FETCH i.createdBy
            LEFT JOIN FETCH i.resolvedBy
            WHERE (:status IS NULL OR i.status = :status)
              AND (:assetId IS NULL OR i.asset.id = :assetId)
              AND (:idFromFolio IS NULL OR i.id = :idFromFolio)
            ORDER BY i.incidentDate DESC, i.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(i) FROM Incident i
            WHERE (:status IS NULL OR i.status = :status)
              AND (:assetId IS NULL OR i.asset.id = :assetId)
              AND (:idFromFolio IS NULL OR i.id = :idFromFolio)
            """)
    Page<Incident> findAllFiltered(
            @Param("status") IncidentStatus status,
            @Param("assetId") Long assetId,
            @Param("idFromFolio") Long idFromFolio,
            Pageable pageable);

    /** Cuenta incidencias abiertas o en progreso para un bien. Útil antes de crear una nueva. */
    @Query("""
            SELECT COUNT(i) FROM Incident i
            WHERE i.asset.id = :assetId
              AND i.status IN (mx.edu.unpa.inventory_backend.enums.IncidentStatus.OPEN,
                               mx.edu.unpa.inventory_backend.enums.IncidentStatus.IN_PROGRESS)
            """)
    long countActiveByAssetId(@Param("assetId") Long assetId);

    /**
     * Lista sin paginar de todas las incidencias de un bien.
     * Para el tab del detalle cuando el cliente maneja la paginación en frontend.
     */
    @Query("""
            SELECT i FROM Incident i
            JOIN FETCH i.createdBy
            LEFT JOIN FETCH i.resolvedBy
            WHERE i.asset.id = :assetId
            ORDER BY i.createdAt DESC
            """)
    List<Incident> findAllByAssetId(@Param("assetId") Long assetId);
    long countByStatus(IncidentStatus status);
}