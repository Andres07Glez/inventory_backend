package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.domains.Invoice;
import mx.edu.unpa.inventory_backend.domains.Supplier;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.InvoiceRepository;
import mx.edu.unpa.inventory_backend.repositories.SupplierRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock private InvoiceRepository  invoiceRepository;
    @Mock private AssetRepository    assetRepository;
    @Mock private UserRepository     userRepository;
    @Mock private SupplierRepository supplierRepository;
    @Mock private StorageService     storageService;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    // ── Fixtures ──────────────────────────────────────────────

    private Supplier supplier;
    private User     creator;
    private Invoice  invoice;
    private InvoiceRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        Guardian guardian = new Guardian();
        guardian.setId(1L);
        guardian.setFullName("Juan Pérez");

        // User usa @Builder para respetar los @Builder.Default (role, isActive)
        creator = User.builder()
                .id(1L)
                .username("admin")
                .passwordHash("hash")
                .guardian(guardian)
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        supplier = new Supplier();
        supplier.setId(10L);
        supplier.setName("Proveedor S.A.");

        invoice = new Invoice();
        invoice.setId(1L);
        invoice.setInvoiceNumber("FAC-2024-001");
        invoice.setSupplier(supplier);
        invoice.setInvoiceDate(LocalDate.of(2024, 6, 1));
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setNotes("Notas de prueba");
        invoice.setCreatedBy(creator);
        invoice.setDocumentPath(null);

        validRequest = new InvoiceRequestDTO();
        validRequest.setInvoiceNumber("FAC-2024-001");
        validRequest.setSupplierId(10L);
        validRequest.setInvoiceDate(LocalDate.of(2024, 6, 1));
        validRequest.setTotalAmount(BigDecimal.valueOf(5000.00));
        validRequest.setNotes("Notas de prueba");
    }

    // ════════════════════════════════════════════════════════════
    // getAll
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAll")
    class GetAll {

        @Test
        @DisplayName("should_returnPagedResults_when_queryIsNull")
        void should_returnPagedResults_when_queryIsNull() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Invoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
            when(invoiceRepository.findAllByOrderByInvoiceDateDesc(pageable)).thenReturn(page);

            // Act
            Page<InvoiceResponseDTO> result = invoiceService.getAll(null, pageable);

            // Assert
            assertEquals(1, result.getTotalElements());
            assertEquals("FAC-2024-001", result.getContent().get(0).getInvoiceNumber());
            verify(invoiceRepository).findAllByOrderByInvoiceDateDesc(pageable);
            verify(invoiceRepository, never()).search(any(), any());
        }

        @Test
        @DisplayName("should_returnPagedResults_when_queryIsBlankString")
        void should_returnPagedResults_when_queryIsBlankString() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Invoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
            when(invoiceRepository.findAllByOrderByInvoiceDateDesc(pageable)).thenReturn(page);

            // Act
            Page<InvoiceResponseDTO> result = invoiceService.getAll("   ", pageable);

            // Assert
            verify(invoiceRepository).findAllByOrderByInvoiceDateDesc(pageable);
            verify(invoiceRepository, never()).search(any(), any());
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("should_delegateToSearch_when_queryHasText")
        void should_delegateToSearch_when_queryHasText() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            Page<Invoice> page = new PageImpl<>(List.of(invoice), pageable, 1);
            when(invoiceRepository.search("FAC", pageable)).thenReturn(page);

            // Act
            Page<InvoiceResponseDTO> result = invoiceService.getAll("FAC", pageable);

            // Assert
            verify(invoiceRepository).search("FAC", pageable);
            verify(invoiceRepository, never()).findAllByOrderByInvoiceDateDesc(any(Pageable.class));
            assertEquals(1, result.getTotalElements());
        }

        @Test
        @DisplayName("should_trimQueryBeforeSearch_when_queryHasLeadingSpaces")
        void should_trimQueryBeforeSearch_when_queryHasLeadingSpaces() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(invoiceRepository.search("FAC", pageable))
                    .thenReturn(new PageImpl<>(List.of(invoice), pageable, 1));

            // Act
            invoiceService.getAll("  FAC  ", pageable);

            // Assert — el servicio debe pasar "FAC" sin espacios
            verify(invoiceRepository).search("FAC", pageable);
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noInvoicesExist")
        void should_returnEmptyPage_when_noInvoicesExist() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(invoiceRepository.findAllByOrderByInvoiceDateDesc(pageable))
                    .thenReturn(Page.empty(pageable));

            // Act
            Page<InvoiceResponseDTO> result = invoiceService.getAll(null, pageable);

            // Assert
            assertTrue(result.isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════
    // getAllUnpaged
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAllUnpaged")
    class GetAllUnpaged {

        @Test
        @DisplayName("should_returnAllInvoicesAsList_when_called")
        void should_returnAllInvoicesAsList_when_called() {
            // Arrange
            Invoice second = new Invoice();
            second.setId(2L);
            second.setInvoiceNumber("FAC-2024-002");
            second.setSupplier(supplier);
            second.setInvoiceDate(LocalDate.of(2024, 7, 1));
            second.setTotalAmount(BigDecimal.ZERO);
            second.setCreatedBy(creator);

            when(invoiceRepository.findAllByOrderByInvoiceDateDesc())
                    .thenReturn(List.of(invoice, second));

            // Act
            List<InvoiceResponseDTO> result = invoiceService.getAllUnpaged();

            // Assert
            assertEquals(2, result.size());
            assertEquals("FAC-2024-001", result.get(0).getInvoiceNumber());
            assertEquals("FAC-2024-002", result.get(1).getInvoiceNumber());
        }

        @Test
        @DisplayName("should_returnEmptyList_when_noInvoicesExist")
        void should_returnEmptyList_when_noInvoicesExist() {
            when(invoiceRepository.findAllByOrderByInvoiceDateDesc()).thenReturn(List.of());

            List<InvoiceResponseDTO> result = invoiceService.getAllUnpaged();

            assertTrue(result.isEmpty());
        }
    }

    // ════════════════════════════════════════════════════════════
    // getById
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById")
    class GetById {

        @Test
        @DisplayName("should_returnDTO_when_invoiceExists")
        void should_returnDTO_when_invoiceExists() {
            // Arrange
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act
            InvoiceResponseDTO result = invoiceService.getById(1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("FAC-2024-001", result.getInvoiceNumber());
            assertEquals(10L, result.getSupplierId());
            assertEquals("Proveedor S.A.", result.getSupplierName());
        }

        @Test
        @DisplayName("should_mapSupplierFieldsToNull_when_invoiceHasNoSupplier")
        void should_mapSupplierFieldsToNull_when_invoiceHasNoSupplier() {
            // Arrange
            invoice.setSupplier(null);
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act
            InvoiceResponseDTO result = invoiceService.getById(1L);

            // Assert
            assertNull(result.getSupplierId());
            assertNull(result.getSupplierName());
        }

        @Test
        @DisplayName("should_buildDocumentUrl_when_invoiceHasDocumentPath")
        void should_buildDocumentUrl_when_invoiceHasDocumentPath() {
            // Arrange
            invoice.setDocumentPath("invoices/1/abc.pdf");
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(storageService.buildPublicUrl("invoices/1/abc.pdf"))
                    .thenReturn("http://localhost:8080/uploads/invoices/1/abc.pdf");

            // Act
            InvoiceResponseDTO result = invoiceService.getById(1L);

            // Assert
            assertEquals("http://localhost:8080/uploads/invoices/1/abc.pdf", result.getDocumentUrl());
            verify(storageService).buildPublicUrl("invoices/1/abc.pdf");
        }

        @Test
        @DisplayName("should_returnNullDocumentUrl_when_invoiceHasNoDocumentPath")
        void should_returnNullDocumentUrl_when_invoiceHasNoDocumentPath() {
            // Arrange — documentPath ya es null en el fixture
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            // Act
            InvoiceResponseDTO result = invoiceService.getById(1L);

            // Assert
            assertNull(result.getDocumentUrl());
            verify(storageService, never()).buildPublicUrl(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_invoiceNotFound")
        void should_throwResourceNotFoundException_when_invoiceNotFound() {
            when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.getById(99L)
            );
            assertTrue(ex.getMessage().contains("99"));
        }
    }

    // ════════════════════════════════════════════════════════════
    // create
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("should_returnCreatedDTO_when_requestIsValid")
        void should_returnCreatedDTO_when_requestIsValid() {
            // Arrange
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase("FAC-2024-001")).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

            // Act
            InvoiceResponseDTO result = invoiceService.create(validRequest, 1L);

            // Assert
            assertNotNull(result);
            assertEquals("FAC-2024-001", result.getInvoiceNumber());
            assertEquals(10L, result.getSupplierId());
            verify(invoiceRepository).save(any(Invoice.class));
        }

        @Test
        @DisplayName("should_setCreatedByOnInvoice_when_userIsFound")
        void should_setCreatedByOnInvoice_when_userIsFound() {
            // Arrange
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase(any())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            invoiceService.create(validRequest, 1L);

            // Assert — verificamos que save recibió una entidad con createdBy asignado
            verify(invoiceRepository).save(argThat(saved ->
                    saved.getCreatedBy() != null && saved.getCreatedBy().getId().equals(1L)
            ));
        }

        @Test
        @DisplayName("should_trimInvoiceNumber_when_requestHasTrailingSpaces")
        void should_trimInvoiceNumber_when_requestHasTrailingSpaces() {
            // Arrange
            validRequest.setInvoiceNumber("  FAC-2024-001  ");
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase("FAC-2024-001")).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            invoiceService.create(validRequest, 1L);

            // Assert
            verify(invoiceRepository).save(argThat(saved ->
                    "FAC-2024-001".equals(saved.getInvoiceNumber())
            ));
        }

        @Test
        @DisplayName("should_throwDuplicateResourceException_when_invoiceNumberAlreadyExists")
        void should_throwDuplicateResourceException_when_invoiceNumberAlreadyExists() {
            // Arrange
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase("FAC-2024-001")).thenReturn(true);

            // Act & Assert
            DuplicateResourceException ex = assertThrows(
                    DuplicateResourceException.class,
                    () -> invoiceService.create(validRequest, 1L)
            );
            assertTrue(ex.getMessage().contains("FAC-2024-001"));
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_userNotFoundOrInactive")
        void should_throwResourceNotFoundException_when_userNotFoundOrInactive() {
            // Arrange
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase(any())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.create(validRequest, 1L)
            );
            assertTrue(ex.getMessage().contains("1"));
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_supplierNotFound")
        void should_throwResourceNotFoundException_when_supplierNotFound() {
            // Arrange
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase(any())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(supplierRepository.findById(10L)).thenReturn(Optional.empty());

            // Act & Assert
            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.create(validRequest, 1L)
            );
            assertTrue(ex.getMessage().contains("10"));
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_setTotalAmountToZero_when_creating")
        void should_setTotalAmountToZero_when_creating() {
            // totalAmount del request se ignora; el servicio fija BigDecimal.ZERO
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase(any())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(creator));
            when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
            when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

            invoiceService.create(validRequest, 1L);

            verify(invoiceRepository).save(argThat(saved ->
                    BigDecimal.ZERO.compareTo(saved.getTotalAmount()) == 0
            ));
        }
    }

    // ════════════════════════════════════════════════════════════
    // update
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("should_returnUpdatedDTO_when_requestIsValid")
        void should_returnUpdatedDTO_when_requestIsValid() {
            // Arrange
            InvoiceRequestDTO updateRequest = new InvoiceRequestDTO();
            updateRequest.setInvoiceNumber("FAC-2024-002");
            updateRequest.setSupplierId(10L);
            updateRequest.setInvoiceDate(LocalDate.of(2024, 8, 1));
            updateRequest.setNotes("Notas actualizadas");

            Invoice updated = new Invoice();
            updated.setId(1L);
            updated.setInvoiceNumber("FAC-2024-002");
            updated.setSupplier(supplier);
            updated.setInvoiceDate(LocalDate.of(2024, 8, 1));
            updated.setTotalAmount(BigDecimal.ZERO);
            updated.setCreatedBy(creator);

            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase("FAC-2024-002")).thenReturn(false);
            when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(updated);

            // Act
            InvoiceResponseDTO result = invoiceService.update(1L, updateRequest);

            // Assert
            assertEquals("FAC-2024-002", result.getInvoiceNumber());
            verify(invoiceRepository).save(any(Invoice.class));
        }

        @Test
        @DisplayName("should_allowUpdate_when_invoiceNumberIsUnchanged")
        void should_allowUpdate_when_invoiceNumberIsUnchanged() {
            // El número no cambia → no debe consultar existsByInvoiceNumberIgnoreCase
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
            when(invoiceRepository.save(any(Invoice.class))).thenReturn(invoice);

            invoiceService.update(1L, validRequest); // mismo número "FAC-2024-001"

            // Assert — el check de duplicados no debe dispararse
            verify(invoiceRepository, never()).existsByInvoiceNumberIgnoreCase(any());
        }

        @Test
        @DisplayName("should_throwDuplicateResourceException_when_newNumberBelongsToAnotherInvoice")
        void should_throwDuplicateResourceException_when_newNumberBelongsToAnotherInvoice() {
            // Arrange — invoice actual tiene "FAC-2024-001", el request pide "FAC-2024-099"
            InvoiceRequestDTO updateRequest = new InvoiceRequestDTO();
            updateRequest.setInvoiceNumber("FAC-2024-099");
            updateRequest.setSupplierId(10L);
            updateRequest.setInvoiceDate(LocalDate.now());

            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.existsByInvoiceNumberIgnoreCase("FAC-2024-099")).thenReturn(true);

            // Act & Assert
            assertThrows(
                    DuplicateResourceException.class,
                    () -> invoiceService.update(1L, updateRequest)
            );
            verify(invoiceRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_invoiceToUpdateNotFound")
        void should_throwResourceNotFoundException_when_invoiceToUpdateNotFound() {
            when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.update(99L, validRequest)
            );
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_supplierNotFoundOnUpdate")
        void should_throwResourceNotFoundException_when_supplierNotFoundOnUpdate() {
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(supplierRepository.findById(10L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.update(1L, validRequest)
            );
            verify(invoiceRepository, never()).save(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // delete
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should_deleteInvoice_when_noAssetsAreLinked")
        void should_deleteInvoice_when_noAssetsAreLinked() {
            // Arrange
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(assetRepository.existsByInvoiceId(1L)).thenReturn(false);

            // Act
            invoiceService.delete(1L);

            // Assert
            verify(invoiceRepository).delete(invoice);
        }

        @Test
        @DisplayName("should_throwIllegalStateException_when_invoiceHasAssociatedAssets")
        void should_throwIllegalStateException_when_invoiceHasAssociatedAssets() {
            // Arrange
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(assetRepository.existsByInvoiceId(1L)).thenReturn(true);

            // Act & Assert
            IllegalStateException ex = assertThrows(
                    IllegalStateException.class,
                    () -> invoiceService.delete(1L)
            );
            assertTrue(ex.getMessage().contains("bienes asociados"));
            verify(invoiceRepository, never()).delete(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_invoiceToDeleteNotFound")
        void should_throwResourceNotFoundException_when_invoiceToDeleteNotFound() {
            when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.delete(99L)
            );
            verify(invoiceRepository, never()).delete(any());
        }
    }

    // ════════════════════════════════════════════════════════════
    // uploadPdf
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("uploadPdf")
    class UploadPdf {

        private MockMultipartFile validPdf;

        @BeforeEach
        void setUpPdf() {
            validPdf = new MockMultipartFile(
                    "file", "factura.pdf", "application/pdf",
                    new byte[100]
            );
        }

        @Test
        @DisplayName("should_returnPublicUrl_when_pdfIsValidAndInvoiceExists")
        void should_returnPublicUrl_when_pdfIsValidAndInvoiceExists() {
            // Arrange
            // CAMBIO: Ahora usamos existsByIdAndIsActiveTrue
            when(userRepository.existsByIdAndIsActiveTrue(1L)).thenReturn(true);
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(storageService.store(validPdf, "invoices/1")).thenReturn("invoices/1/uuid.pdf");
            when(storageService.buildPublicUrl("invoices/1/uuid.pdf"))
                    .thenReturn("http://localhost:8080/uploads/invoices/1/uuid.pdf");
            when(invoiceRepository.save(any())).thenReturn(invoice);

            // Act
            String url = invoiceService.uploadPdf(1L, validPdf, 1L);

            // Assert
            assertEquals("http://localhost:8080/uploads/invoices/1/uuid.pdf", url);
            verify(storageService).store(validPdf, "invoices/1");
            verify(invoiceRepository).save(argThat(saved ->
                    "invoices/1/uuid.pdf".equals(saved.getDocumentPath())
            ));
        }

        @Test
        @DisplayName("should_deleteOldPdf_when_invoiceAlreadyHasDocumentPath")
        void should_deleteOldPdf_when_invoiceAlreadyHasDocumentPath() {
            // Arrange
            invoice.setDocumentPath("invoices/1/old.pdf");
            // CAMBIO: Ahora usamos existsByIdAndIsActiveTrue
            when(userRepository.existsByIdAndIsActiveTrue(1L)).thenReturn(true);
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(storageService.store(any(), any())).thenReturn("invoices/1/new.pdf");
            when(storageService.buildPublicUrl(any())).thenReturn("http://...");
            when(invoiceRepository.save(any())).thenReturn(invoice);

            // Act
            invoiceService.uploadPdf(1L, validPdf, 1L);

            // Assert
            verify(storageService).delete("invoices/1/old.pdf");
        }

        @Test
        @DisplayName("should_notDeleteOldPdf_when_invoiceHasNoDocumentPath")
        void should_notDeleteOldPdf_when_invoiceHasNoDocumentPath() {
            // invoice.documentPath == null en el fixture
            // CAMBIO: Ahora usamos existsByIdAndIsActiveTrue
            when(userRepository.existsByIdAndIsActiveTrue(1L)).thenReturn(true);
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(storageService.store(any(), any())).thenReturn("invoices/1/new.pdf");
            when(storageService.buildPublicUrl(any())).thenReturn("http://...");
            when(invoiceRepository.save(any())).thenReturn(invoice);

            invoiceService.uploadPdf(1L, validPdf, 1L);

            verify(storageService, never()).delete(any());
        }

        @Test
        @DisplayName("should_throwResponseStatusException_when_fileIsEmpty")
        void should_throwResponseStatusException_when_fileIsEmpty() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.pdf", "application/pdf", new byte[0]
            );

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> invoiceService.uploadPdf(1L, emptyFile, 1L)
            );
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should_throwResponseStatusException_when_fileIsNull")
        void should_throwResponseStatusException_when_fileIsNull() {
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> invoiceService.uploadPdf(1L, null, 1L)
            );
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        @DisplayName("should_throwResponseStatusException_when_mimeTypeIsNotPdf")
        void should_throwResponseStatusException_when_mimeTypeIsNotPdf() {
            MockMultipartFile notPdf = new MockMultipartFile(
                    "file", "imagen.png", "image/png", new byte[100]
            );

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> invoiceService.uploadPdf(1L, notPdf, 1L)
            );
            assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getStatusCode());
        }

        @Test
        @DisplayName("should_throwResponseStatusException_when_fileSizeExceedsLimit")
        void should_throwResponseStatusException_when_fileSizeExceedsLimit() {
            // 20 MB + 1 byte
            byte[] oversized = new byte[20 * 1024 * 1024 + 1];
            MockMultipartFile bigFile = new MockMultipartFile(
                    "file", "grande.pdf", "application/pdf", oversized
            );

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> invoiceService.uploadPdf(1L, bigFile, 1L)
            );
            assertEquals(HttpStatus.CONTENT_TOO_LARGE, ex.getStatusCode());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_userNotActiveOnUpload")
        void should_throwResourceNotFoundException_when_userNotActiveOnUpload() {
            // CAMBIO: Retornamos false simulando que no existe/no está activo
            when(userRepository.existsByIdAndIsActiveTrue(1L)).thenReturn(false);

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.uploadPdf(1L, validPdf, 1L)
            );
            verify(invoiceRepository, never()).findById(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_invoiceNotFoundOnUpload")
        void should_throwResourceNotFoundException_when_invoiceNotFoundOnUpload() {
            // CAMBIO: Ahora usamos existsByIdAndIsActiveTrue
            when(userRepository.existsByIdAndIsActiveTrue(1L)).thenReturn(true);
            when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.uploadPdf(99L, validPdf, 1L)
            );
        }
    }

    // ════════════════════════════════════════════════════════════
    // deletePdf
    // ════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deletePdf")
    class DeletePdf {

        @Test
        @DisplayName("should_deletePdfAndClearPath_when_invoiceHasDocumentPath")
        void should_deletePdfAndClearPath_when_invoiceHasDocumentPath() {
            // Arrange
            invoice.setDocumentPath("invoices/1/old.pdf");
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
            when(invoiceRepository.save(any())).thenReturn(invoice);

            // Act
            invoiceService.deletePdf(1L);

            // Assert
            verify(storageService).delete("invoices/1/old.pdf");
            verify(invoiceRepository).save(argThat(saved ->
                    saved.getDocumentPath() == null
            ));
        }

        @Test
        @DisplayName("should_throwResponseStatusException_when_invoiceHasNoDocumentPath")
        void should_throwResponseStatusException_when_invoiceHasNoDocumentPath() {
            // invoice.documentPath == null en el fixture
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> invoiceService.deletePdf(1L)
            );
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            verify(storageService, never()).delete(any());
        }

        @Test
        @DisplayName("should_throwResponseStatusException_when_documentPathIsBlank")
        void should_throwResponseStatusException_when_documentPathIsBlank() {
            invoice.setDocumentPath("   ");
            when(invoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));

            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> invoiceService.deletePdf(1L)
            );
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_invoiceNotFoundOnDeletePdf")
        void should_throwResourceNotFoundException_when_invoiceNotFoundOnDeletePdf() {
            when(invoiceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(
                    ResourceNotFoundException.class,
                    () -> invoiceService.deletePdf(99L)
            );
            verify(storageService, never()).delete(any());
        }
    }
}