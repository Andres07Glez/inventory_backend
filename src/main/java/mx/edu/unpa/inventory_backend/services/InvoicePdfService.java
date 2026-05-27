package mx.edu.unpa.inventory_backend.services;

import org.springframework.web.multipart.MultipartFile;

public interface InvoicePdfService {

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
