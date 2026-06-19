package mx.edu.unpa.inventory_backend.dtos.dashboard.response;

import mx.edu.unpa.inventory_backend.enums.Campus;

public record LocationStatDTO(
        String locationName,
        Campus campus,
        Long   assetCount
) {}
