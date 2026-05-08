package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.dtos.category.request.CategoryRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.category.response.CategoryResponseDTO;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId",   source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryResponseDTO toDto(Category category);

    /**
     * Solo mapea los campos escalares; el campo {@code parent} se resuelve
     * manualmente en el service porque requiere una consulta al repositorio.
     */
    @Mapping(target = "parent",   ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "id",       ignore = true)
    Category toEntity(CategoryRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "parent",   ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "id",       ignore = true)
    void updateEntityFromDto(CategoryRequestDTO dto, @MappingTarget Category category);
}
