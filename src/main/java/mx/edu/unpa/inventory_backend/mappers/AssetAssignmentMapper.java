package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.AssetAssignment;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetAssignmentResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AssetAssignmentMapper {

    @Mapping(target = "assetInventoryNumber", source = "asset.inventoryNumber")
    @Mapping(target = "assetDescription", source = "asset.description")
    @Mapping(target = "guardianName", source = "guardian.fullName")
    @Mapping(target = "locationName", source = "location.name")
    AssetAssignmentResponseDTO toDto(AssetAssignment assignment);
}
