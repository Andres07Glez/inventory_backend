package mx.edu.unpa.inventory_backend.dtos.assetAssigment.response;

import java.time.LocalDateTime;

public record AssignmentHistoryResponse(
        Long            id,
        String          guardianName,
        String          guardianEmployeeNumber,
        String          locationName,
        LocalDateTime   assignedAt,
        LocalDateTime returnedAt,           // null si sigue activa
        String          assignedByUsername,
        String          notes
) {}
