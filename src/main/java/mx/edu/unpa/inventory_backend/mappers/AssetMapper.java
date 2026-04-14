package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.DTOs.Asset.Request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.DTOs.Asset.Response.AssetResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AssetMapper {

    // Convierte el Domain a un ResponseDTO para enviarlo al cliente
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "location.name", target = "locationName")
    @Mapping(source = "location.campus", target = "campus")
    @Mapping(source = "invoice.invoiceNumber", target = "invoiceNumber")
    @Mapping(source = "createdBy.fullName", target = "createdByName")
    AssetResponseDTO toResponseDto(Asset asset);

    // Convierte el RequestDTO a Domain para guardarlo (ignora los objetos complejos por ahora)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Asset toEntity(AssetRequestDTO requestDto);

}
