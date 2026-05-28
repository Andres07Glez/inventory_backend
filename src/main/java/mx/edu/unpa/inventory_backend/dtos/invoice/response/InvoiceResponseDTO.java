package mx.edu.unpa.inventory_backend.dtos.invoice.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class InvoiceResponseDTO {

    private Long          id;
    private String        invoiceNumber;
    private Long          supplierId;
    private String        supplierName;
    private LocalDate invoiceDate;
    private BigDecimal totalAmount;
    /**
     * Ruta relativa almacenada en BD (invoices/{id}/{uuid}.pdf).
     * Se mantiene para compatibilidad interna.
     */
    private String        documentPath;

    /**
     * URL pública lista para usar directamente desde el frontend.
     * Ejemplo: http://localhost:8080/uploads/invoices/3/abc123.pdf
     */
    private String        documentUrl;

    private String        notes;
    private LocalDateTime createdAt;
    private String        createdByName;

}
