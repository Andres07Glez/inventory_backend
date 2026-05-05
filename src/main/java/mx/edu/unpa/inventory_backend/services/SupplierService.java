package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.supplier.request.SupplierRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.supplier.response.SupplierResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface SupplierService {

    /** Crea un nuevo proveedor. Lanza {@link mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException} si el nombre ya existe. */
    SupplierResponseDTO create(SupplierRequestDTO request);

    /** Retorna la página de proveedores activos. */
    Page<SupplierResponseDTO> findAllActive(Pageable pageable);

    /** Búsqueda por nombre o nombre de contacto (solo activos). */
    Page<SupplierResponseDTO> search(String q, Pageable pageable);

    /** Busca un proveedor por ID. Lanza {@link mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException} si no existe. */
    SupplierResponseDTO findById(Long id);

    /** Actualiza los datos de un proveedor existente. */
    SupplierResponseDTO update(Long id, SupplierRequestDTO request);

    /**
     * Desactiva un proveedor (baja lógica).
     * No elimina el registro para preservar el historial de facturas.
     */
    void deactivate(Long id);
}
