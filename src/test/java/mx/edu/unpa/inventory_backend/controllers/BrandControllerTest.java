package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.dtos.brand.request.BrandRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.brand.response.BrandResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.BrandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(GlobalExceptionHandler.class)
@WebMvcTest(
        controllers = BrandController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BrandController")
class BrandControllerTest {

    private static final String BASE_URL = "/v1/brands";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private BrandService brandService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private BrandResponseDTO dellBrand;
    private BrandResponseDTO hpBrand;

    @BeforeEach
    void setUp() {
        dellBrand = new BrandResponseDTO(1, "Dell", true);
        hpBrand   = new BrandResponseDTO(2, "HP",   true);
    }

    // ── Helper: construye un BrandRequestDTO con setter (no es record) ────────

    private BrandRequestDTO buildRequest(String name) {
        BrandRequestDTO dto = new BrandRequestDTO();
        dto.setName(name);
        return dto;
    }

    // =========================================================================
    // GET /v1/brands
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/brands")
    class GetAll {

        @Test
        @DisplayName("should_return200WithBrandList_when_activeBrandsExist")
        void should_return200WithBrandList_when_activeBrandsExist() throws Exception {
            when(brandService.getAllActive()).thenReturn(List.of(dellBrand, hpBrand));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].name").value("Dell"))
                    .andExpect(jsonPath("$.data[0].isActive").value(true))
                    .andExpect(jsonPath("$.data[1].id").value(2))
                    .andExpect(jsonPath("$.data[1].name").value("HP"));

            verify(brandService).getAllActive();
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_noActiveBrandsExist")
        void should_return200WithEmptyList_when_noActiveBrandsExist() throws Exception {
            when(brandService.getAllActive()).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // =========================================================================
    // POST /v1/brands
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/brands")
    class Create {

        @Test
        @DisplayName("should_return201WithCreatedBrand_when_requestIsValid")
        void should_return201WithCreatedBrand_when_requestIsValid() throws Exception {
            when(brandService.create(any(BrandRequestDTO.class))).thenReturn(dellBrand);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("Dell"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Dell"))
                    .andExpect(jsonPath("$.data.isActive").value(true));

            verify(brandService).create(any(BrandRequestDTO.class));
        }

        // ── Validaciones de DTO ────────────────────────────────────────────────

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameIsBlank")
        void should_return400WithApiResponse_when_nameIsBlank() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre de la marca es obligatorio."));

            verifyNoInteractions(brandService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameIsNull")
        void should_return400WithApiResponse_when_nameIsNull() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(null))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre de la marca es obligatorio."));

            verifyNoInteractions(brandService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameExceeds100Characters")
        void should_return400WithApiResponse_when_nameExceeds100Characters() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("A".repeat(101)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre no puede superar los 100 caracteres."));

            verifyNoInteractions(brandService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_requestBodyIsMissing")
        void should_return400WithApiResponse_when_requestBodyIsMissing() throws Exception {
            // HttpMessageNotReadableException → 400 (el handler devuelve String, no ApiResponse — code smell conocido)
            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(brandService);
        }

        // ── Excepciones de dominio ─────────────────────────────────────────────

        @Test
        @DisplayName("should_return409WithApiResponse_when_brandNameAlreadyExists")
        void should_return409WithApiResponse_when_brandNameAlreadyExists() throws Exception {
            when(brandService.create(any(BrandRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("Ya existe una marca con el nombre 'Dell'"));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("Dell"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Ya existe una marca con el nombre 'Dell'"));
        }

        // Edge case: nombre con exactamente 100 caracteres — límite permitido
        @Test
        @DisplayName("should_return201_when_nameIsExactly100Characters")
        void should_return201_when_nameIsExactly100Characters() throws Exception {
            String maxName = "A".repeat(100);
            BrandResponseDTO response = new BrandResponseDTO(3, maxName, true);
            when(brandService.create(any(BrandRequestDTO.class))).thenReturn(response);

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(maxName))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.name").value(maxName));
        }
    }

    // =========================================================================

    // =========================================================================

    @Nested
    @DisplayName("PUT /v1/brands/{id}")
    class Update {

        @Test
        @DisplayName("should_return200WithUpdatedBrand_when_requestIsValid")
        void should_return200WithUpdatedBrand_when_requestIsValid() throws Exception {
            BrandResponseDTO updated = new BrandResponseDTO(1, "Dell Technologies", true);
            when(brandService.update(eq(1), any(BrandRequestDTO.class))).thenReturn(updated);

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("Dell Technologies"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Dell Technologies"));

            verify(brandService).update(eq(1), any(BrandRequestDTO.class));
        }

        // ── Validaciones de DTO ────────────────────────────────────────────────

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameIsBlankOnUpdate")
        void should_return400WithApiResponse_when_nameIsBlankOnUpdate() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest(""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre de la marca es obligatorio."));

            verifyNoInteractions(brandService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameExceeds100CharactersOnUpdate")
        void should_return400WithApiResponse_when_nameExceeds100CharactersOnUpdate() throws Exception {
            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("A".repeat(101)))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre no puede superar los 100 caracteres."));

            verifyNoInteractions(brandService);
        }

        // ── Excepciones de dominio ─────────────────────────────────────────────

        @Test
        @DisplayName("should_return404WithApiResponse_when_brandNotFound")
        void should_return404WithApiResponse_when_brandNotFound() throws Exception {
            when(brandService.update(eq(999), any(BrandRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Marca no encontrada con id: 999"));

            mockMvc.perform(put(BASE_URL + "/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("Lenovo"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Marca no encontrada con id: 999"));
        }

        @Test
        @DisplayName("should_return409WithApiResponse_when_updatedNameAlreadyExists")
        void should_return409WithApiResponse_when_updatedNameAlreadyExists() throws Exception {
            when(brandService.update(eq(1), any(BrandRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("Ya existe una marca con el nombre 'HP'"));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(buildRequest("HP"))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Ya existe una marca con el nombre 'HP'"));
        }
    }

    // =========================================================================
    // =========================================================================

    @Nested
    @DisplayName("DELETE /v1/brands/{id}")
    class Delete {

        @Test
        @DisplayName("should_return200WithNullData_when_brandIsSuccessfullyDeleted")
        void should_return200WithNullData_when_brandIsSuccessfullyDeleted() throws Exception {
            // BrandController devuelve ApiResponse.ok(null) — con @JsonInclude(NON_NULL),
            // el campo "data" no se serializa. Distinto de CategoryController que devuelve 204.
            doNothing().when(brandService).delete(1);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").doesNotExist());

            verify(brandService).delete(1);
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_brandNotFoundOnDelete")
        void should_return404WithApiResponse_when_brandNotFoundOnDelete() throws Exception {
            doThrow(new ResourceNotFoundException("Marca no encontrada con id: 999"))
                    .when(brandService).delete(999);

            mockMvc.perform(delete(BASE_URL + "/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Marca no encontrada con id: 999"));
        }

        // Edge case: intentar eliminar una marca asignada a bienes activos.
        // El servicio lanza InvalidAssetStateException → 409.
        @Test
        @DisplayName("should_return409WithApiResponse_when_brandHasActiveAssets")
        void should_return409WithApiResponse_when_brandHasActiveAssets() throws Exception {
            doThrow(new mx.edu.unpa.inventory_backend.exceptions.InvalidAssetStateException(
                    "No se puede eliminar una marca con bienes activos asociados"))
                    .when(brandService).delete(1);

            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("No se puede eliminar una marca con bienes activos asociados"));
        }
    }
}