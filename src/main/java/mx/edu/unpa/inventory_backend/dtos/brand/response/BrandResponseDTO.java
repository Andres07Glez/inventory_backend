package mx.edu.unpa.inventory_backend.dtos.brand.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BrandResponseDTO {

    private Integer id;
    private String  name;
    private Boolean isActive;

}
