package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Location;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    Optional<Location> findByIdAndIsActiveTrue(Integer id);


    /** Lista solo las ubicaciones activas, con soporte de paginación. */
    Page<Location> findByIsActiveTrue(Pageable pageable);

    /** Verifica existencia por nombre exacto dentro de un mismo campus. */
    boolean existsByNameAndCampus(String name, String campus);

    /**
     * Búsqueda por nombre, edificio o campus.
     * Se usa en el endpoint /search?q=...
     */
    @Query("""
            SELECT l FROM Location l
            WHERE l.isActive = true
              AND (LOWER(l.name)     LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(l.building) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(l.campus)   LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Location> searchActive(@Param("q") String q, Pageable pageable);

}
