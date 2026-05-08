package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.dtos.guardian.request.GuardianRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianResponseDTO;
import org.mapstruct.*;


@Mapper(componentModel = "spring")
public interface GuardianMapper {

    @Mapping(target = "locationId",   source = "location.id")
    @Mapping(target = "locationName", source = "location.name")
    GuardianResponseDTO toDto(Guardian guardian);


    @Mapping(target = "location", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    Guardian toEntity(GuardianRequestDTO dto);


    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    void updateEntityFromDto(GuardianRequestDTO dto, @MappingTarget Guardian guardian);
}