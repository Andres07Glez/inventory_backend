package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InvoiceService {

    Page<InvoiceResponseDTO> getAll(String query, Pageable pageable);

    List<InvoiceResponseDTO> getAllUnpaged();

    InvoiceResponseDTO getById(Long id);

    InvoiceResponseDTO create(InvoiceRequestDTO request, Long userId);

    InvoiceResponseDTO update(Long id, InvoiceRequestDTO request);

    void delete(Long id);

}
