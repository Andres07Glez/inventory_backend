package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface InvoiceService {

    Page<InvoiceResponseDTO> getAll(String query, Pageable pageable);

    List<InvoiceResponseDTO> getAllUnpaged();

    InvoiceResponseDTO getById(Long id);

    InvoiceResponseDTO create(InvoiceRequestDTO request, Long userId);

    InvoiceResponseDTO update(Long id, InvoiceRequestDTO request);

    void delete(Long id);

    /**
     * Almacena el PDF en uploads/invoices/{invoiceId}/archivo.pdf
     * y persiste la ruta relativa en Invoice.documentPath.
     * Retorna la URL pública del documento.
     */
    String uploadPdf(Long invoiceId, MultipartFile file, Long uploadedById);

    /**
     * Elimina el PDF físico y limpia Invoice.documentPath.
     */
    void deletePdf(Long invoiceId);

}
