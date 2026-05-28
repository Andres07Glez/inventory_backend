package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.dtos.asset.response.UpdateConditionResponse;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AssetCommandMapper {

    /**
     * Construye la respuesta de confirmación de cambio de condición.
     *
     * @param asset             el bien ya actualizado (con la nueva condición)
     * @param previousCondition la condición ANTES del cambio (se captura en el servicio)
     */
    @Mapping(target = "assetId",           source = "asset.id")
    @Mapping(target = "inventoryNumber",   source = "asset.inventoryNumber")
    @Mapping(target = "previousCondition", source = "previousCondition")
    @Mapping(target = "newCondition",      source = "asset.conditionStatus")
    @Mapping(target = "updatedAt",         source = "asset.updatedAt")
    UpdateConditionResponse toUpdateConditionResponse(Asset asset, ConditionStatus previousCondition);
}
