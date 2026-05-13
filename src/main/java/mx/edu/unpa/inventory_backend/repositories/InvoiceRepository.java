package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByOrderByInvoiceDateDesc();

    Page<Invoice> findAllByOrderByInvoiceDateDesc(Pageable pageable);

    // Búsqueda paginada por ID, número de factura o nombre de proveedor
    @Query("""
            SELECT i FROM Invoice i
            LEFT JOIN i.supplier s
            WHERE CAST(i.id AS string) LIKE %:q%
               OR LOWER(i.invoiceNumber)  LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(s.name)           LIKE LOWER(CONCAT('%', :q, '%'))
            ORDER BY i.invoiceDate DESC
            """)
    Page<Invoice> search(@Param("q") String q, Pageable pageable);

    boolean existsByInvoiceNumberIgnoreCase(String invoiceNumber);

}
