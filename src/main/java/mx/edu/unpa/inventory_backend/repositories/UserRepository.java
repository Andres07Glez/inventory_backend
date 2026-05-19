package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.User;
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
        WHERE LOWER(u.fullName)       LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(u.username)       LIKE LOWER(CONCAT('%', :term, '%'))
           OR LOWER(u.employeeNumber) LIKE LOWER(CONCAT('%', :term, '%'))
        """)
    Page<User> findBySearchTerm(@Param("term") String term, Pageable pageable);
}