package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    Optional<Category> findByIdAndIsActiveTrue(Integer id);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Integer id);

    Page<Category> findByIsActiveTrue(Pageable pageable);

    /**
     * Búsqueda por nombre (solo activas).
     */
    @Query("""
            SELECT c FROM Category c
            WHERE c.isActive = true
              AND LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))
            """)
    Page<Category> searchActive(@Param("q") String q, Pageable pageable);

    /**
     * Lista todas las categorías raíz activas (sin padre), para construir árbol en el frontend.
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND c.parent IS NULL")
    Page<Category> findRootCategories(Pageable pageable);
}
