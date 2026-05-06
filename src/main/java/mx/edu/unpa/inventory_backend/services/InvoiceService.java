package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;

import java.util.List;

public interface InvoiceService {

    List<InvoiceResponseDTO> getAll();

    InvoiceResponseDTO getById(Long id);

    InvoiceResponseDTO create(InvoiceRequestDTO request, Long userId);

    InvoiceResponseDTO update(Long id, InvoiceRequestDTO request);

    void delete(Long id);

}
