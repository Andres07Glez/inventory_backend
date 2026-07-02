package mx.edu.unpa.inventory_backend.controllers;

import tools.jackson.databind.ObjectMapper;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.invoice.request.InvoiceRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.invoice.response.InvoiceResponseDTO;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.FileStorageException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {InvoiceController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        InvoiceControllerTest.TestExceptionHandlerExtension.class,
        InvoiceControllerTest.AuthenticatedUserArgumentResolverConfig.class
})
@DisplayName("InvoiceController — Integration / Slice Tests")
class InvoiceControllerTest {

    // ================================================================
    // Handler local — cubre MethodArgumentTypeMismatchException (400).
    // ================================================================
    @RestControllerAdvice
    static class TestExceptionHandlerExtension {

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
                MethodArgumentTypeMismatchException ex) {
            String message = String.format(
                    "Valor inválido '%s' para el parámetro '%s'",
                    ex.getValue(), ex.getName());
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(message));
        }
    }

    // ================================================================
    // Resolver local para @AuthenticationPrincipal AuthenticatedUser.
    //
    // Con seguridad deshabilitada (@AutoConfigureMockMvc addFilters=false),
    // el SecurityContext está vacío y @AuthenticationPrincipal resuelve null,
    // lo que provoca NullPointerException al invocar currentUser.id().
    // Este resolver inyecta un AuthenticatedUser fijo (id=1L) en cada
    // request de test sin necesidad de levantar el contexto de seguridad.
    // ================================================================
    static class AuthenticatedUserResolver implements HandlerMethodArgumentResolver {

        private static final AuthenticatedUser FAKE_USER = new AuthenticatedUser(
                1L, "test_user", "secret", UserRole.ADMIN, true,1L
        );

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return AuthenticatedUser.class.isAssignableFrom(parameter.getParameterType());
        }

        @Override
        public Object resolveArgument(MethodParameter parameter,
                                      ModelAndViewContainer mavContainer,
                                      NativeWebRequest webRequest,
                                      WebDataBinderFactory binderFactory) {
            return FAKE_USER;
        }
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class AuthenticatedUserArgumentResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(
                List<org.springframework.web.method.support.HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticatedUserResolver());
        }
    }

    // -------------------------------------------------------
    // Infraestructura
    // -------------------------------------------------------

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InvoiceService invoiceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // -------------------------------------------------------
    // Fixtures reutilizables
    // -------------------------------------------------------

    private InvoiceResponseDTO sampleResponse;
    private InvoiceRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new InvoiceResponseDTO(
                1L,
                "FAC-2024-001",
                10L,
                "Proveedor Ejemplo S.A.",
                LocalDate.of(2024, 6, 1),
                new BigDecimal("1500.00"),
                null,
                null,
                "Notas de prueba",
                LocalDateTime.of(2024, 6, 1, 10, 0),
                "test_user"
        );

        validRequest = new InvoiceRequestDTO();
        validRequest.setInvoiceNumber("FAC-2024-001");
        validRequest.setSupplierId(10L);
        validRequest.setInvoiceDate(LocalDate.of(2024, 6, 1));
        validRequest.setTotalAmount(new BigDecimal("1500.00"));
        validRequest.setNotes("Notas de prueba");
    }

    // ================================================================
    // GET /v1/invoices
    // ================================================================

    @Nested
    @DisplayName("GET /v1/invoices")
    class GetAll {

        @Test
        @DisplayName("should_return200WithPage_when_invoicesExist")
        void should_return200WithPage_when_invoicesExist() throws Exception {
            Page<InvoiceResponseDTO> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 10), 1
            );
            when(invoiceService.getAll(eq(null), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/invoices")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].id", is(1)))
                    .andExpect(jsonPath("$.data.content[0].invoiceNumber", is("FAC-2024-001")))
                    .andExpect(jsonPath("$.data.content[0].supplierName", is("Proveedor Ejemplo S.A.")))
                    .andExpect(jsonPath("$.data.totalElements", is(1)));
        }

        @Test
        @DisplayName("should_return200WithFilteredPage_when_queryParamIsProvided")
        void should_return200WithFilteredPage_when_queryParamIsProvided() throws Exception {
            Page<InvoiceResponseDTO> page = new PageImpl<>(List.of(sampleResponse));
            when(invoiceService.getAll(eq("FAC-2024"), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/invoices")
                            .param("q", "FAC-2024"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.content", hasSize(1)))
                    .andExpect(jsonPath("$.data.content[0].invoiceNumber", is("FAC-2024-001")));
        }

        @Test
        @DisplayName("should_return200WithEmptyPage_when_noInvoicesExist")
        void should_return200WithEmptyPage_when_noInvoicesExist() throws Exception {
            Page<InvoiceResponseDTO> emptyPage = Page.empty(PageRequest.of(0, 10));
            when(invoiceService.getAll(any(), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/v1/invoices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.content", hasSize(0)))
                    .andExpect(jsonPath("$.data.totalElements", is(0)));
        }
    }

    // ================================================================
    // GET /v1/invoices/id
    // ================================================================

    @Nested
    @DisplayName("GET /v1/invoices/{id}")
    class GetById {

        @Test
        @DisplayName("should_return200WithInvoice_when_idExists")
        void should_return200WithInvoice_when_idExists() throws Exception {
            when(invoiceService.getById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/v1/invoices/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.invoiceNumber", is("FAC-2024-001")))
                    .andExpect(jsonPath("$.data.supplierId", is(10)))
                    .andExpect(jsonPath("$.data.supplierName", is("Proveedor Ejemplo S.A.")))
                    .andExpect(jsonPath("$.data.totalAmount", is(1500.00)))
                    .andExpect(jsonPath("$.data.createdByName", is("test_user")));
        }

        @Test
        @DisplayName("should_return404_when_idDoesNotExist")
        void should_return404_when_idDoesNotExist() throws Exception {
            when(invoiceService.getById(999L))
                    .thenThrow(new ResourceNotFoundException("Factura no encontrada con id: 999"));

            mockMvc.perform(get("/v1/invoices/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("999")));
        }
    }

    // ================================================================
    // POST /v1/invoices
    // ================================================================

    @Nested
    @DisplayName("POST /v1/invoices")
    class Create {

        @Test
        @DisplayName("should_return201WithCreatedInvoice_when_requestIsValid")
        void should_return201WithCreatedInvoice_when_requestIsValid() throws Exception {
            // userId=1L inyectado por AuthenticatedUserResolver
            when(invoiceService.create(any(InvoiceRequestDTO.class), eq(1L)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.invoiceNumber", is("FAC-2024-001")));
        }

        @Test
        @DisplayName("should_return400_when_invoiceNumberIsBlank")
        void should_return400_when_invoiceNumberIsBlank() throws Exception {
            validRequest.setInvoiceNumber("");

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", notNullValue()));
        }

        @Test
        @DisplayName("should_return400_when_invoiceNumberExceedsMaxLength")
        void should_return400_when_invoiceNumberExceedsMaxLength() throws Exception {
            validRequest.setInvoiceNumber("F".repeat(101));

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_supplierIdIsNull")
        void should_return400_when_supplierIdIsNull() throws Exception {
            validRequest.setSupplierId(null);

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_invoiceDateIsNull")
        void should_return400_when_invoiceDateIsNull() throws Exception {
            validRequest.setInvoiceDate(null);

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_totalAmountIsZero")
        void should_return400_when_totalAmountIsZero() throws Exception {
            validRequest.setTotalAmount(BigDecimal.ZERO);

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_notesExceedMaxLength")
        void should_return400_when_notesExceedMaxLength() throws Exception {
            validRequest.setNotes("N".repeat(1001));

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissing")
        void should_return400_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return409_when_invoiceNumberAlreadyExists")
        void should_return409_when_invoiceNumberAlreadyExists() throws Exception {
            when(invoiceService.create(any(InvoiceRequestDTO.class), eq(1L)))
                    .thenThrow(new DuplicateResourceException(
                            "Ya existe una factura con el número 'FAC-2024-001'"));

            mockMvc.perform(post("/v1/invoices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("FAC-2024-001")));
        }
    }

    // ================================================================
    // PUT /v1/invoices/id
    // ================================================================

    @Nested
    @DisplayName("PUT /v1/invoices/{id}")
    class Update {

        @Test
        @DisplayName("should_return200WithUpdatedInvoice_when_requestIsValid")
        void should_return200WithUpdatedInvoice_when_requestIsValid() throws Exception {
            InvoiceResponseDTO updated = new InvoiceResponseDTO(
                    1L, "FAC-2024-002", 10L, "Proveedor Ejemplo S.A.",
                    LocalDate.of(2024, 7, 1), new BigDecimal("2000.00"),
                    null, null, "Notas actualizadas",
                    LocalDateTime.of(2024, 6, 1, 10, 0), "test_user"
            );
            when(invoiceService.update(eq(1L), any(InvoiceRequestDTO.class))).thenReturn(updated);

            validRequest.setInvoiceNumber("FAC-2024-002");
            validRequest.setTotalAmount(new BigDecimal("2000.00"));

            mockMvc.perform(put("/v1/invoices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.invoiceNumber", is("FAC-2024-002")))
                    .andExpect(jsonPath("$.data.totalAmount", is(2000.00)));
        }

        @Test
        @DisplayName("should_return404_when_invoiceToUpdateDoesNotExist")
        void should_return404_when_invoiceToUpdateDoesNotExist() throws Exception {
            when(invoiceService.update(eq(999L), any(InvoiceRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Factura no encontrada con id: 999"));

            mockMvc.perform(put("/v1/invoices/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_updateRequestBodyHasBlankInvoiceNumber")
        void should_return400_when_updateRequestBodyHasBlankInvoiceNumber() throws Exception {
            validRequest.setInvoiceNumber("   ");

            mockMvc.perform(put("/v1/invoices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_updateRequestBodyIsMissing")
        void should_return400_when_updateRequestBodyIsMissing() throws Exception {
            mockMvc.perform(put("/v1/invoices/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return409_when_updatedInvoiceNumberAlreadyExists")
        void should_return409_when_updatedInvoiceNumberAlreadyExists() throws Exception {
            when(invoiceService.update(eq(1L), any(InvoiceRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException(
                            "Ya existe una factura con ese número"));

            mockMvc.perform(put("/v1/invoices/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    // ================================================================
    // DELETE /v1/invoices/id
    // ================================================================

    @Nested
    @DisplayName("DELETE /v1/invoices/{id}")
    class Delete {

        @Test
        @DisplayName("should_return200WithOkResponse_when_invoiceIsSuccessfullyDeleted")
        void should_return200WithOkResponse_when_invoiceIsSuccessfullyDeleted() throws Exception {
            doNothing().when(invoiceService).delete(1L);

            mockMvc.perform(delete("/v1/invoices/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            verify(invoiceService, times(1)).delete(1L);
        }

        @Test
        @DisplayName("should_return404_when_invoiceToDeleteDoesNotExist")
        void should_return404_when_invoiceToDeleteDoesNotExist() throws Exception {
            doThrow(new ResourceNotFoundException("Factura no encontrada con id: 99"))
                    .when(invoiceService).delete(99L);

            mockMvc.perform(delete("/v1/invoices/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // ================================================================
    // POST /v1/invoices/{id}/pdf
    // ================================================================

    @Nested
    @DisplayName("POST /v1/invoices/{id}/pdf")
    class UploadPdf {

        @Test
        @DisplayName("should_return200WithPublicUrl_when_pdfIsSuccessfullyUploaded")
        void should_return200WithPublicUrl_when_pdfIsSuccessfullyUploaded() throws Exception {
            String publicUrl = "http://localhost:8080/uploads/invoices/1/abc123.pdf";
            when(invoiceService.uploadPdf(eq(1L), any(), eq(1L))).thenReturn(publicUrl);

            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "factura.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "contenido-pdf-falso".getBytes()
            );

            mockMvc.perform(multipart("/v1/invoices/1/pdf").file(pdf))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", is(publicUrl)));
        }

        @Test
        @DisplayName("should_return404_when_invoiceForPdfUploadDoesNotExist")
        void should_return404_when_invoiceForPdfUploadDoesNotExist() throws Exception {
            when(invoiceService.uploadPdf(eq(999L), any(), eq(1L)))
                    .thenThrow(new ResourceNotFoundException("Factura no encontrada con id: 999"));

            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "factura.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "contenido-pdf-falso".getBytes()
            );

            mockMvc.perform(multipart("/v1/invoices/999/pdf").file(pdf))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("999")));
        }

        @Test
        @DisplayName("should_return500_when_fileStorageFailsDuringUpload")
        void should_return500_when_fileStorageFailsDuringUpload() throws Exception {
            when(invoiceService.uploadPdf(eq(1L), any(), eq(1L)))
                    .thenThrow(new FileStorageException("Error al guardar el archivo en disco"));

            MockMultipartFile pdf = new MockMultipartFile(
                    "file", "factura.pdf",
                    MediaType.APPLICATION_PDF_VALUE,
                    "contenido-pdf-falso".getBytes()
            );

            mockMvc.perform(multipart("/v1/invoices/1/pdf").file(pdf))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    // ================================================================
    // DELETE /v1/invoices/{id}/pdf
    // ================================================================

    @Nested
    @DisplayName("DELETE /v1/invoices/{id}/pdf")
    class DeletePdf {

        @Test
        @DisplayName("should_return200WithConfirmationMessage_when_pdfIsSuccessfullyDeleted")
        void should_return200WithConfirmationMessage_when_pdfIsSuccessfullyDeleted() throws Exception {
            doNothing().when(invoiceService).deletePdf(1L);

            mockMvc.perform(delete("/v1/invoices/1/pdf"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", is("Documento PDF eliminado correctamente.")));

            verify(invoiceService, times(1)).deletePdf(1L);
        }

        @Test
        @DisplayName("should_return404_when_invoiceForPdfDeleteDoesNotExist")
        void should_return404_when_invoiceForPdfDeleteDoesNotExist() throws Exception {
            doThrow(new ResourceNotFoundException("Factura no encontrada con id: 99"))
                    .when(invoiceService).deletePdf(99L);

            mockMvc.perform(delete("/v1/invoices/99/pdf"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }

        @Test
        @DisplayName("should_return500_when_fileStorageFailsDuringDeletion")
        void should_return500_when_fileStorageFailsDuringDeletion() throws Exception {
            doThrow(new FileStorageException("No se pudo eliminar el archivo del disco"))
                    .when(invoiceService).deletePdf(1L);

            mockMvc.perform(delete("/v1/invoices/1/pdf"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }
}