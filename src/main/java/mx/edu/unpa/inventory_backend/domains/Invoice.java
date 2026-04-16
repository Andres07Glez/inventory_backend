package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Getter
@Setter
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 100)
    private String invoiceNumber; // Número de factura del proveedor

    @Column(length = 200)
    private String supplier; // Nombre del proveedor

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate; // Fecha impresa en la factura

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "document_path", length = 500)
    private String documentPath; // Ruta al PDF o imagen digitalizada

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy; // Usuario que registró la factura

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}
