package mx.edu.unpa.inventory_backend.dtos.incident.response;

import java.time.LocalDateTime;

public record IncidentImageResponseDTO(
        Long id,
        String fileName,
        String url,
        String mimeType,
        LocalDateTime uploadedAt,
        String uploadedByName
) {}
