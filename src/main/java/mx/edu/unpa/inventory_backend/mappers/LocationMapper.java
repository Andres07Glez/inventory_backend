package mx.edu.unpa.inventory_backend.mappers;


import mx.edu.unpa.inventory_backend.domains.Location;
import mx.edu.unpa.inventory_backend.dtos.location.request.LocationRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.location.response.LocationResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper MapStruct para la entidad Location.
 */
@Mapper(componentModel = "spring")
public interface LocationMapper {

    /** Convierte la entidad a su DTO de respuesta. */
    LocationResponseDTO toDto(Location location);

    /**
     * Convierte el DTO de entrada a una entidad nueva.
     * isActive se asigna explícitamente en el service.
     */
    Location toEntity(LocationRequestDTO dto);

    /**
     * Actualiza solo los campos no nulos del DTO sobre la entidad existente.
     * Útil para el endpoint PUT.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(LocationRequestDTO dto, @MappingTarget Location location);
}