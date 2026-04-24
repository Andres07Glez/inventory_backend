package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.dtos.guardian.request.GuardianRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * Mapper MapStruct para la entidad Guardian.
 *
 * componentModel = "spring"  → genera un bean Spring inyectable con @Autowired / @RequiredArgsConstructor.
 */
@Mapper(componentModel = "spring")
public interface GuardianMapper {

    /** Convierte la entidad a su DTO de respuesta. */
    GuardianResponseDTO toDto(Guardian guardian);

    /**
     * Convierte el DTO de entrada a una entidad nueva.
     * El campo isActive no viene en el request, se inicializa en el service (true por defecto).
     */
    Guardian toEntity(GuardianRequestDTO dto);

    /**
     * Actualiza solo los campos no nulos de un DTO sobre una entidad existente.
     * Útil para el endpoint PATCH / PUT parcial.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(GuardianRequestDTO dto, @MappingTarget Guardian guardian);
}