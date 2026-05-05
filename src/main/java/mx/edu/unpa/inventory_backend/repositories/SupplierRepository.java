package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Page<Supplier> findByIsActiveTrue(Pageable pageable);

    /**
     * Búsqueda por nombre o nombre de contacto (solo activos).
     */
    @Query("""
            SELECT s FROM Supplier s
            WHERE s.isActive = true
              AND (LOWER(s.name)        LIKE LOWER(CONCAT('%', :q, '%'))
               OR  LOWER(s.contactName) LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Supplier> searchActive(@Param("q") String q, Pageable pageable);
}
