package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.domains.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
public interface LocationRepository extends JpaRepository<Location, Integer> {

    Optional<Location> findByIdAndIsActiveTrue(Integer id);
}