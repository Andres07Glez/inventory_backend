package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    Optional<User> findByIdAndIsActiveTrue(Long id);
    boolean existsByUsername(String username);
    boolean existsByGuardianId(Long guardianId);
    boolean existsByIdAndIsActiveTrue(Long id);

    @Query("SELECT u FROM User u JOIN u.guardian g WHERE g.employeeNumber = :employeeNumber")
    Optional<User> findByGuardianEmployeeNumber(@Param("employeeNumber") String employeeNumber);

    @Query("SELECT COUNT(u) > 0 FROM User u JOIN u.guardian g WHERE g.employeeNumber = :employeeNumber")
    boolean existsByGuardianEmployeeNumber(@Param("employeeNumber") String employeeNumber);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN u.guardian g
        WHERE (:search IS NULL
               OR LOWER(g.fullName)    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.username)    LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(g.email)       LIKE LOWER(CONCAT('%', :search, '%'))
               OR g.employeeNumber     LIKE CONCAT('%', :search, '%'))
        AND   (:role     IS NULL OR u.role     = :role)
        AND   (:isActive IS NULL OR u.isActive = :isActive)
        """)
    Page<User> findWithFilters(
            @Param("search")   String   search,
            @Param("role")     UserRole role,
            @Param("isActive") Boolean  isActive,
            Pageable pageable
    );
}