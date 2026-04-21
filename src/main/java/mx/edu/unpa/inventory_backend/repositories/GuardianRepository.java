package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {
    /** Busca por número de empleado (campo único). */
    Optional<Guardian> findByEmployeeNumber(String employeeNumber);

    /** Verifica existencia por número de empleado (útil para validaciones). */
    boolean existsByEmployeeNumber(String employeeNumber);

    /** Lista solo los resguardantes activos, con soporte de paginación. */
    Page<Guardian> findByIsActiveTrue(Pageable pageable);

    /**
     * Búsqueda por nombre, número de empleado o departamento.
     * Se usa en el endpoint de búsqueda general (/search?q=...).
     */
    @Query("""
            SELECT g FROM Guardian g
            WHERE g.isActive = true
              AND (LOWER(g.fullName)       LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(g.employeeNumber) LIKE LOWER(CONCAT('%', :q, '%'))
                OR LOWER(g.department)     LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Guardian> searchActive(@Param("q") String q, Pageable pageable);
}
