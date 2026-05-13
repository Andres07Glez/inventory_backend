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
    private String        documentPath;
    private String        notes;
    private LocalDateTime createdAt;
    private String        createdByName;

}
