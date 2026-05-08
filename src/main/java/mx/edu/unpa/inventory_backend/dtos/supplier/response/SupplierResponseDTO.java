package mx.edu.unpa.inventory_backend.dtos.supplier.response;

import java.time.LocalDateTime;

public record SupplierResponseDTO(
        Long           id,
        String         name,
        String         rfc,
        String         contactName,
        String         email,
        String         phone,
        String         address,
        String         notes,
        Boolean        isActive,
        LocalDateTime  createdAt,
        LocalDateTime  updatedAt
) {}
