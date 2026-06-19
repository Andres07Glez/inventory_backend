package mx.edu.unpa.inventory_backend.controllers;

import tools.jackson.databind.ObjectMapper;
import mx.edu.unpa.inventory_backend.dtos.supplier.request.SupplierRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.supplier.response.SupplierResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {SupplierController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SupplierController — Slice Tests")
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SupplierService supplierService;

    // --- Security beans required to avoid context failure ---
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ------------------------------------------------------------------ //
    //  Fixtures
    // ------------------------------------------------------------------ //

    private SupplierResponseDTO supplierResponse;
    private SupplierRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        supplierResponse = new SupplierResponseDTO(
                1L,
                "Proveedor Demo S.A.",
                "XAXX010101000",
                "Juan Pérez",
                "contacto@demo.com",
                "5512345678",
                "Av. Siempre Viva 123",
                "Notas de prueba",
                true,
                LocalDateTime.of(2024, 1, 15, 10, 0),
                LocalDateTime.of(2024, 6, 1, 12, 30)
        );

        validRequest = new SupplierRequestDTO(
                "Proveedor Demo S.A.",
                "Juan Pérez",
                "contacto@demo.com",
                "5512345678",
                "Av. Siempre Viva 123",
                "Notas de prueba",
                "XAXX010101000"
        );
    }

    // ================================================================== //
    //  GET /v1/suppliers
    // ================================================================== //

    @Nested
    @DisplayName("GET /v1/suppliers — findAllActive")
    class FindAllActive {

        @Test
        @DisplayName("should_return200WithPage_when_suppliersExist")
        void should_return200WithPage_when_suppliersExist() throws Exception {
            Pageable pageable = PageRequest.of(0, 10);
            Page<SupplierResponseDTO> page = new PageImpl<>(List.of(supplierResponse), pageable, 1);

            when(supplierService.findAllActive(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/suppliers")
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].name", is("Proveedor Demo S.A.")))
                    .andExpect(jsonPath("$.content[0].rfc", is("XAXX010101000")))
                    .andExpect(jsonPath("$.content[0].isActive", is(true)))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("should_return200WithEmptyPage_when_noSuppliersExist")
        void should_return200WithEmptyPage_when_noSuppliersExist() throws Exception {
            Page<SupplierResponseDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(supplierService.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/v1/suppliers"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    // ================================================================== //
    //  GET /v1/suppliers/search
    // ================================================================== //

    @Nested
    @DisplayName("GET /v1/suppliers/search — search")
    class Search {

        @Test
        @DisplayName("should_return200WithResults_when_queryMatchesSuppliers")
        void should_return200WithResults_when_queryMatchesSuppliers() throws Exception {
            Page<SupplierResponseDTO> page = new PageImpl<>(List.of(supplierResponse), PageRequest.of(0, 10), 1);

            when(supplierService.search(eq("Demo"), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/suppliers/search")
                            .param("q", "Demo")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", is("Proveedor Demo S.A.")));
        }

        @Test
        @DisplayName("should_return400_when_queryParamIsMissing")
        void should_return400_when_queryParamIsMissing() throws Exception {
            mockMvc.perform(get("/v1/suppliers/search"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return400_when_queryParamIsBlank")
        void should_return400_when_queryParamIsBlank() throws Exception {
            mockMvc.perform(get("/v1/suppliers/search")
                            .param("q", "   "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return200WithEmptyPage_when_noMatchFound")
        void should_return200WithEmptyPage_when_noMatchFound() throws Exception {
            Page<SupplierResponseDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(supplierService.search(eq("inexistente"), any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/v1/suppliers/search")
                            .param("q", "inexistente"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)));
        }
    }

    // ================================================================== //
    //  GET /v1/suppliers/id
    // ================================================================== //

    @Nested
    @DisplayName("GET /v1/suppliers/{id} — findById")
    class FindById {

        @Test
        @DisplayName("should_return200WithSupplier_when_idExists")
        void should_return200WithSupplier_when_idExists() throws Exception {
            when(supplierService.findById(1L)).thenReturn(supplierResponse);

            mockMvc.perform(get("/v1/suppliers/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.name", is("Proveedor Demo S.A.")))
                    .andExpect(jsonPath("$.data.rfc", is("XAXX010101000")))
                    .andExpect(jsonPath("$.data.contactName", is("Juan Pérez")))
                    .andExpect(jsonPath("$.data.email", is("contacto@demo.com")))
                    .andExpect(jsonPath("$.data.phone", is("5512345678")))
                    .andExpect(jsonPath("$.data.isActive", is(true)));
        }

        @Test
        @DisplayName("should_return404_when_idNotFound")
        void should_return404_when_idNotFound() throws Exception {
            when(supplierService.findById(99L))
                    .thenThrow(new ResourceNotFoundException("Proveedor no encontrado con id: 99"));

            mockMvc.perform(get("/v1/suppliers/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }

    // ================================================================== //
    //  POST /v1/suppliers
    // ================================================================== //

    @Nested
    @DisplayName("POST /v1/suppliers — create")
    class Create {

        @Test
        @DisplayName("should_return201WithCreatedSupplier_when_requestIsValid")
        void should_return201WithCreatedSupplier_when_requestIsValid() throws Exception {
            when(supplierService.create(any(SupplierRequestDTO.class))).thenReturn(supplierResponse);

            mockMvc.perform(post("/v1/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.name", is("Proveedor Demo S.A.")));
        }

        @Test
        @DisplayName("should_return400_when_nameIsBlank")
        void should_return400_when_nameIsBlank() throws Exception {
            SupplierRequestDTO badRequest = new SupplierRequestDTO(
                    "",           // name vacío — @NotBlank
                    "Juan Pérez",
                    "contacto@demo.com",
                    "5512345678",
                    "Av. Siempre Viva 123",
                    null,
                    "XAXX010101000"
            );

            mockMvc.perform(post("/v1/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_emailIsInvalidFormat")
        void should_return400_when_emailIsInvalidFormat() throws Exception {
            SupplierRequestDTO badRequest = new SupplierRequestDTO(
                    "Proveedor Demo S.A.",
                    "Juan Pérez",
                    "email-invalido",   // @Email falla
                    "5512345678",
                    "Av. Siempre Viva 123",
                    null,
                    "XAXX010101000"
            );

            mockMvc.perform(post("/v1/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_rfcFormatIsInvalid")
        void should_return400_when_rfcFormatIsInvalid() throws Exception {
            SupplierRequestDTO badRequest = new SupplierRequestDTO(
                    "Proveedor Demo S.A.",
                    "Juan Pérez",
                    "contacto@demo.com",
                    "5512345678",
                    "Av. Siempre Viva 123",
                    null,
                    "RFC-INVALIDO"    // @Pattern falla
            );

            mockMvc.perform(post("/v1/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissing")
        void should_return400_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/v1/suppliers")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return409_when_supplierNameAlreadyExists")
        void should_return409_when_supplierNameAlreadyExists() throws Exception {
            when(supplierService.create(any(SupplierRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("Ya existe un proveedor con el nombre: Proveedor Demo S.A."));

            mockMvc.perform(post("/v1/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("Proveedor Demo S.A.")));
        }

        @Test
        @DisplayName("should_return201_when_optionalFieldsAreNull")
        void should_return201_when_optionalFieldsAreNull() throws Exception {
            SupplierRequestDTO minimalRequest = new SupplierRequestDTO(
                    "Solo Nombre S.A.",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

            SupplierResponseDTO minimalResponse = new SupplierResponseDTO(
                    2L, "Solo Nombre S.A.", null, null, null,
                    null, null, null, true,
                    LocalDateTime.now(), LocalDateTime.now()
            );

            when(supplierService.create(any(SupplierRequestDTO.class))).thenReturn(minimalResponse);

            mockMvc.perform(post("/v1/suppliers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(minimalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(2)))
                    .andExpect(jsonPath("$.data.name", is("Solo Nombre S.A.")));
        }
    }

    // ================================================================== //
    //  PUT /v1/suppliers/id
    // ================================================================== //

    @Nested
    @DisplayName("PUT /v1/suppliers/{id} — update")
    class Update {

        @Test
        @DisplayName("should_return200WithUpdatedSupplier_when_requestIsValid")
        void should_return200WithUpdatedSupplier_when_requestIsValid() throws Exception {
            SupplierResponseDTO updated = new SupplierResponseDTO(
                    1L, "Nombre Actualizado S.A.", "XAXX010101000",
                    "Nuevo Contacto", "nuevo@demo.com", "5599887766",
                    "Nueva Dirección", null, true,
                    LocalDateTime.of(2024, 1, 15, 10, 0),
                    LocalDateTime.of(2024, 6, 10, 9, 0)
            );

            SupplierRequestDTO updateRequest = new SupplierRequestDTO(
                    "Nombre Actualizado S.A.",
                    "Nuevo Contacto",
                    "nuevo@demo.com",
                    "5599887766",
                    "Nueva Dirección",
                    null,
                    "XAXX010101000"
            );

            when(supplierService.update(eq(1L), any(SupplierRequestDTO.class))).thenReturn(updated);

            mockMvc.perform(put("/v1/suppliers/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.name", is("Nombre Actualizado S.A.")))
                    .andExpect(jsonPath("$.data.contactName", is("Nuevo Contacto")));
        }

        @Test
        @DisplayName("should_return404_when_supplierIdNotFound")
        void should_return404_when_supplierIdNotFound() throws Exception {
            when(supplierService.update(eq(99L), any(SupplierRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Proveedor no encontrado con id: 99"));

            mockMvc.perform(put("/v1/suppliers/99")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }

        @Test
        @DisplayName("should_return400_when_nameIsBlankOnUpdate")
        void should_return400_when_nameIsBlankOnUpdate() throws Exception {
            SupplierRequestDTO badRequest = new SupplierRequestDTO(
                    "",
                    "Contacto",
                    null, null, null, null, null
            );

            mockMvc.perform(put("/v1/suppliers/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissingOnUpdate")
        void should_return400_when_requestBodyIsMissingOnUpdate() throws Exception {
            mockMvc.perform(put("/v1/suppliers/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return409_when_updatedNameAlreadyExistsInAnotherSupplier")
        void should_return409_when_updatedNameAlreadyExistsInAnotherSupplier() throws Exception {
            when(supplierService.update(eq(1L), any(SupplierRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("Ya existe un proveedor con el nombre: Nombre Actualizado S.A."));

            mockMvc.perform(put("/v1/suppliers/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    // ================================================================== //
    //  DELETE /v1/suppliers/id
    // ================================================================== //

    @Nested
    @DisplayName("DELETE /v1/suppliers/{id} — deactivate")
    class Deactivate {

        @Test
        @DisplayName("should_return204_when_supplierIsSuccessfullyDeactivated")
        void should_return204_when_supplierIsSuccessfullyDeactivated() throws Exception {
            doNothing().when(supplierService).deactivate(1L);

            mockMvc.perform(delete("/v1/suppliers/1"))
                    .andExpect(status().isNoContent());

            verify(supplierService, times(1)).deactivate(1L);
        }

        @Test
        @DisplayName("should_return404_when_supplierToDeactivateNotFound")
        void should_return404_when_supplierToDeactivateNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Proveedor no encontrado con id: 99"))
                    .when(supplierService).deactivate(99L);

            mockMvc.perform(delete("/v1/suppliers/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }
}