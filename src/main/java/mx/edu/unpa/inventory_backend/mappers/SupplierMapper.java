package mx.edu.unpa.inventory_backend.mappers;

import mx.edu.unpa.inventory_backend.domains.Supplier;
import mx.edu.unpa.inventory_backend.dtos.supplier.request.SupplierRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.supplier.response.SupplierResponseDTO;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface SupplierMapper {

    SupplierResponseDTO toDto(Supplier supplier);

    Supplier toEntity(SupplierRequestDTO dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(SupplierRequestDTO dto, @MappingTarget Supplier supplier);
}
