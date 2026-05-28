package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentImageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface IncidentImageService {

    List<IncidentImageResponseDTO> getByIncidentId(Long incidentId);

    IncidentImageResponseDTO upload(Long incidentId, MultipartFile file, Long uploadedById) throws IOException;

    void delete(Long incidentId, Long imageId) throws IOException;
}
