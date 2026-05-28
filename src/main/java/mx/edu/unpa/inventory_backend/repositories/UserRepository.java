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
    Optional<User> findByEmployeeNumber(String employeeNumber);
    Optional<User> findByIdAndIsActiveTrue(Long id);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmployeeNumber(String employeeNumber);
    @Query("""
    SELECT u FROM User u
    WHERE (:search IS NULL
           OR LOWER(u.fullName)       LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.username)       LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(u.email)          LIKE LOWER(CONCAT('%', :search, '%'))
           OR u.employeeNumber        LIKE CONCAT('%', :search, '%'))
    AND   (:role     IS NULL OR u.role     = :role)
    AND   (:isActive IS NULL OR u.isActive = :isActive)
    """)
    Page<User> findWithFilters(
            @Param("search")   String  search,
            @Param("role") UserRole role,
            @Param("isActive") Boolean isActive,
            Pageable pageable
    );
}