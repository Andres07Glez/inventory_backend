package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.category.request.CategoryRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.category.response.CategoryResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    /** Crea una nueva categoría. Lanza {@link mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException} si el nombre ya existe. */
    CategoryResponseDTO create(CategoryRequestDTO request);

    /** Retorna la página de categorías activas. */
    Page<CategoryResponseDTO> findAllActive(Pageable pageable);

    /** Búsqueda por nombre (solo activas). */
    Page<CategoryResponseDTO> search(String q, Pageable pageable);

    /** Busca una categoría por ID. Lanza {@link mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException} si no existe o está inactiva. */
    CategoryResponseDTO findById(Integer id);

    /** Actualiza los datos de una categoría existente. */
    CategoryResponseDTO update(Integer id, CategoryRequestDTO request);

    /**
     * Desactiva una categoría (baja lógica).
     * No elimina el registro para preservar la integridad con los bienes existentes.
     */
    void deactivate(Integer id);
}
