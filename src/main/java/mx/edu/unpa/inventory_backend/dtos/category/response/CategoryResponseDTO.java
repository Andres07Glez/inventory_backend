package mx.edu.unpa.inventory_backend.dtos.category.response;

/**
 * DTO de salida para categoría.
 * parentId y parentName permiten reconstruir la jerarquía en el cliente
 * sin exponer la entidad directamente.
 */
public record CategoryResponseDTO(
        Integer id,
        String  name,
        String  description,
        Integer parentId,
        String  parentName,
        Boolean isActive
) {}
