package mx.edu.unpa.inventory_backend.repositories;

import mx.edu.unpa.inventory_backend.domains.Invoice;
import mx.edu.unpa.inventory_backend.domains.Supplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.test.context.ActiveProfiles;


import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// SIN @TestPropertySource — ya está en application-test.properties
@DataJpaTest
@ActiveProfiles("test")
class InvoiceRepositoryTest extends BaseRepositoryTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    // -------------------------------------------------------------------------
    // Helpers específicos de este test
    // -------------------------------------------------------------------------

    /**
     * Supplier mínimo válido.
     * Supplier tiene @PrePersist para createdAt/updatedAt → se maneja solo al persistir.
     */
    private Supplier persistSupplier(String name) {
        Supplier s = new Supplier();
        s.setName(name);
        return entityManager.persistAndFlush(s);
    }

    /**
     * Invoice con supplier y fecha explícita.
     * createdBy es NOT NULL → usamos operatorUser de BaseRepositoryTest.
     */
    private Invoice persistInvoice(String invoiceNumber, LocalDate invoiceDate, Supplier supplier) {
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(invoiceNumber);
        inv.setInvoiceDate(invoiceDate);
        inv.setSupplier(supplier);
        inv.setCreatedBy(operatorUser);           // NOT NULL — obligatorio
        return entityManager.persistAndFlush(inv);
    }

    /**
     * Invoice SIN proveedor — cubre el caso supplier = null en el LEFT JOIN.
     */
    private Invoice persistInvoiceWithoutSupplier(String invoiceNumber, LocalDate invoiceDate) {
        Invoice inv = new Invoice();
        inv.setInvoiceNumber(invoiceNumber);
        inv.setInvoiceDate(invoiceDate);
        inv.setCreatedBy(operatorUser);
        return entityManager.persistAndFlush(inv);
    }

    // =========================================================================
    // existsByInvoiceNumberIgnoreCase
    // =========================================================================

    @Test
    void should_returnTrue_when_invoiceNumberExistsWithSameCase() {
        Supplier supplier = persistSupplier("Proveedor A");
        persistInvoice("FAC-2024-001", LocalDate.now(), supplier);

        boolean exists = invoiceRepository.existsByInvoiceNumberIgnoreCase("FAC-2024-001");

        assertThat(exists).isTrue();
    }

    @Test
    void should_returnTrue_when_invoiceNumberExistsWithDifferentCase() {
        Supplier supplier = persistSupplier("Proveedor B");
        persistInvoice("FAC-2024-002", LocalDate.now(), supplier);

        boolean exists = invoiceRepository.existsByInvoiceNumberIgnoreCase("fac-2024-002");

        assertThat(exists).isTrue();
    }

    @Test
    void should_returnFalse_when_invoiceNumberDoesNotExist() {
        boolean exists = invoiceRepository.existsByInvoiceNumberIgnoreCase("NO-EXISTE-999");

        assertThat(exists).isFalse();
    }

    // =========================================================================
    // findAllByOrderByInvoiceDateDesc (sin paginación)
    // NOTA: este método carga todas las facturas en memoria — riesgo en producción
    //       con volumen alto. Se prueba el comportamiento de ordenamiento.
    // =========================================================================

    @Test
    void should_returnInvoicesOrderedByDateDesc_when_multipleInvoicesExist() {
        Supplier supplier = persistSupplier("Proveedor Orden");
        LocalDate older  = LocalDate.of(2023, 1, 10);
        LocalDate middle = LocalDate.of(2023, 6, 15);
        LocalDate newer  = LocalDate.of(2024, 3, 20);

        persistInvoice("FAC-OLD",    older,  supplier);
        persistInvoice("FAC-MIDDLE", middle, supplier);
        persistInvoice("FAC-NEW",    newer,  supplier);

        // Problema 4: limpiar caché para forzar lectura desde BD
        entityManager.clear();

        List<Invoice> result = invoiceRepository.findAllByOrderByInvoiceDateDesc();

        assertThat(result)
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-NEW", "FAC-MIDDLE", "FAC-OLD");
    }

    @Test
    void should_returnEmptyList_when_noInvoicesExist() {
        List<Invoice> result = invoiceRepository.findAllByOrderByInvoiceDateDesc();

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // findAllByOrderByInvoiceDateDesc (con paginación)
    // =========================================================================

    @Test
    void should_returnFirstPageOrderedByDateDesc_when_invoicesExceedPageSize() {
        Supplier supplier = persistSupplier("Proveedor Paginado");
        persistInvoice("FAC-P1", LocalDate.of(2024, 1, 1), supplier);
        persistInvoice("FAC-P2", LocalDate.of(2024, 2, 1), supplier);
        persistInvoice("FAC-P3", LocalDate.of(2024, 3, 1), supplier);

        entityManager.clear();

        Page<Invoice> page = invoiceRepository.findAllByOrderByInvoiceDateDesc(PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        // La página 0 debe traer las más recientes
        assertThat(page.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-P3", "FAC-P2");
    }

    @Test
    void should_returnSecondPage_when_requestingPageIndex1() {
        Supplier supplier = persistSupplier("Proveedor Página2");
        persistInvoice("FAC-Q1", LocalDate.of(2024, 1, 1), supplier);
        persistInvoice("FAC-Q2", LocalDate.of(2024, 2, 1), supplier);
        persistInvoice("FAC-Q3", LocalDate.of(2024, 3, 1), supplier);

        entityManager.clear();

        Page<Invoice> secondPage = invoiceRepository.findAllByOrderByInvoiceDateDesc(PageRequest.of(1, 2));

        assertThat(secondPage.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-Q1");
    }

    // =========================================================================
    // search — target principal (@Query JPQL con LEFT JOIN)
    // =========================================================================

    // --- búsqueda por invoiceNumber ---

    @Test
    void should_findInvoice_when_queryMatchesInvoiceNumber() {
        Supplier supplier = persistSupplier("Proveedor Búsqueda");
        persistInvoice("FAC-BUSQ-001", LocalDate.now(), supplier);

        Page<Invoice> result = invoiceRepository.search("BUSQ-001", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-BUSQ-001");
    }

    @Test
    void should_findInvoice_when_queryMatchesInvoiceNumberCaseInsensitive() {
        Supplier supplier = persistSupplier("Proveedor Case");
        persistInvoice("FAC-UPPER-001", LocalDate.now(), supplier);

        Page<Invoice> result = invoiceRepository.search("fac-upper", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getInvoiceNumber()).isEqualTo("FAC-UPPER-001");
    }

    // --- búsqueda por ID (CAST AS string) ---

    @Test
    void should_findInvoice_when_queryMatchesId() {
        Supplier supplier = persistSupplier("Proveedor ID");
        Invoice invoice = persistInvoice("FAC-ID-001", LocalDate.now(), supplier);

        entityManager.clear();

        // Buscar por el ID real generado — verifica que CAST(i.id AS string) funciona en H2
        Page<Invoice> result = invoiceRepository.search(
                invoice.getId().toString(), PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Invoice::getId)
                .contains(invoice.getId());
    }

    // --- búsqueda por nombre de proveedor ---

    @Test
    void should_findInvoice_when_queryMatchesSupplierName() {
        Supplier techSupplier = persistSupplier("TecnoMex S.A.");
        persistInvoice("FAC-TECH-001", LocalDate.now(), techSupplier);

        Page<Invoice> result = invoiceRepository.search("TecnoMex", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-TECH-001");
    }

    @Test
    void should_findInvoice_when_queryMatchesSupplierNameCaseInsensitive() {
        Supplier supplier = persistSupplier("Distribuidora Norte");
        persistInvoice("FAC-NORTE-001", LocalDate.now(), supplier);

        Page<Invoice> result = invoiceRepository.search("distribuidora", PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
    }

    // --- edge case: factura sin proveedor no debe romper el LEFT JOIN ---

    @Test
    void should_notExcludeInvoice_when_supplierIsNull() {
        // LEFT JOIN garantiza que facturas sin proveedor aparezcan si coincide otro campo
        persistInvoiceWithoutSupplier("FAC-SIN-PROV", LocalDate.now());

        Page<Invoice> result = invoiceRepository.search("SIN-PROV", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-SIN-PROV");
    }

    @Test
    void should_notReturnInvoiceWithNullSupplier_when_queryTargetsSupplierName() {
        // Factura sin proveedor no debe aparecer cuando se busca por nombre de proveedor
        persistInvoiceWithoutSupplier("FAC-NULL-SUP", LocalDate.now());

        Page<Invoice> result = invoiceRepository.search("NombreInexistente", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
    }

    // --- ordenamiento en search ---

    @Test
    void should_returnResultsOrderedByInvoiceDateDesc_when_searchReturnsMultiple() {
        Supplier supplier = persistSupplier("Proveedor Orden Search");
        persistInvoice("FAC-ORD-A", LocalDate.of(2023, 5, 1), supplier);
        persistInvoice("FAC-ORD-B", LocalDate.of(2024, 5, 1), supplier);
        persistInvoice("FAC-ORD-C", LocalDate.of(2022, 5, 1), supplier);

        // Problema 4: limpiar caché antes de consultar orden desde BD
        entityManager.clear();

        Page<Invoice> result = invoiceRepository.search("FAC-ORD", PageRequest.of(0, 10));

        assertThat(result.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-ORD-B", "FAC-ORD-A", "FAC-ORD-C");
    }

    // --- sin resultados ---

    @Test
    void should_returnEmptyPage_when_queryMatchesNothing() {
        Supplier supplier = persistSupplier("Proveedor Vacío");
        persistInvoice("FAC-VACIO-001", LocalDate.now(), supplier);

        Page<Invoice> result = invoiceRepository.search("XYZ_NADA_999", PageRequest.of(0, 10));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // --- múltiples coincidencias ---

    @Test
    void should_returnMultipleInvoices_when_queryMatchesSeveralRecords() {
        Supplier supplier = persistSupplier("MultiProveedor");
        persistInvoice("FAC-MULTI-001", LocalDate.of(2024, 1, 1), supplier);
        persistInvoice("FAC-MULTI-002", LocalDate.of(2024, 2, 1), supplier);
        persistInvoiceWithoutSupplier("FAC-MULTI-003", LocalDate.of(2024, 3, 1));

        entityManager.clear();

        Page<Invoice> result = invoiceRepository.search("MULTI", PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    // --- paginación en search ---

    @Test
    void should_paginateSearchResults_when_matchesExceedPageSize() {
        Supplier supplier = persistSupplier("Proveedor Pag-Search");
        for (int i = 1; i <= 5; i++) {
            persistInvoice("FAC-PAG-00" + i,
                    LocalDate.of(2024, i, 1), supplier);
        }

        entityManager.clear();

        Page<Invoice> firstPage = invoiceRepository.search("FAC-PAG", PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
        // Orden DESC: FAC-PAG-005 (mayo) y FAC-PAG-004 (abril) primeros
        assertThat(firstPage.getContent())
                .extracting(Invoice::getInvoiceNumber)
                .containsExactly("FAC-PAG-005", "FAC-PAG-004");
    }
}