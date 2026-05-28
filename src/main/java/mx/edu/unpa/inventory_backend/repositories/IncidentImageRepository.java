package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.IncidentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IncidentImageRepository extends JpaRepository<IncidentImage, Long> {

    @Query("""
            SELECT img FROM IncidentImage img
            JOIN FETCH img.uploadedBy
            WHERE img.incident.id = :incidentId
            ORDER BY img.uploadedAt ASC
            """)
    List<IncidentImage> findByIncidentIdOrdered(@Param("incidentId") Long incidentId);

    Optional<IncidentImage> findByIdAndIncidentId(Long id, Long incidentId);

    long countByIncidentId(Long incidentId);
}

