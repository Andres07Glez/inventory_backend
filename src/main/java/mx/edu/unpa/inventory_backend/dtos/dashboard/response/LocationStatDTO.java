package mx.edu.unpa.inventory_backend.dtos.dashboard.response;

public record LocationStatDTO(
        String locationName,
        String campus,
        Long   assetCount
) {}
