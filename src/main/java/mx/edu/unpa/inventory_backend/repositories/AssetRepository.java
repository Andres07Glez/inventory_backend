package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    /**
     * Busca un bien por su número de inventario único (ej. INV-2026-00001).
     * Útil para validaciones antes de registrar uno nuevo.
     */
    Optional<Asset> findByInventoryNumber(String inventoryNumber);

    /**
     * Busca un bien por su código de barras.
     * Útil para la implementación de escaneo con dispositivos móviles.
     */
    Optional<Asset> findByBarcode(String barcode);

    /**
     * Busca bienes por su número de serie.
     * Útil para rastrear equipos de cómputo específicos.
     */
    List<Asset> findBySerialNumber(String serialNumber);

    /**
     * Lista todos los bienes que tienen un estado de ciclo de vida específico.
     * Ejemplo: buscar todos los activos que están 'ASSIGNED'.
     */
    List<Asset> findByLifecycleStatus(String lifecycleStatus);

    /**
     * Busca bienes registrados por un usuario específico.
     * Útil para auditoría de movimientos.
     */
    List<Asset> findByCreatedById(Long userId);

    /**
     * Verifica si ya existe un número de inventario para evitar duplicados.
     */
    boolean existsByInventoryNumber(String inventoryNumber);
}