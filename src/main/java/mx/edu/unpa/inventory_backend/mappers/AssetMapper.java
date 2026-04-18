package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetAssignment;
import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetResumeResponse;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AssetMapper {


    @Mapping(target = "id",           source = "asset.id")
    @Mapping(target = "categoryName", source = "asset.category.name")
    @Mapping(target = "locationName", source = "asset.location.name")
    @Mapping(target = "building",     source = "asset.location.building")
    @Mapping(target = "campus",       source = "asset.location.campus")
    @Mapping(target = "guardian",     source = "activeAssignment", qualifiedByName = "assignmentToGuardianSummary")
    AssetDetailResponse toDetailResponse(Asset asset, AssetAssignment activeAssignment);


    @Named("assignmentToGuardianSummary")
    default GuardianSummary assignmentToGuardianSummary(AssetAssignment assignment) {
        if (assignment == null) return null;

        Guardian guardian = assignment.getGuardian();
        if (guardian == null) return null;

        return new GuardianSummary(
                guardian.getId(),
                guardian.getFullName(),
                guardian.getEmployeeNumber(),
                guardian.getDepartment()
        );
    }
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "locationName", source = "location.name")
    AssetResumeResponse toDto(Asset asset);
}
