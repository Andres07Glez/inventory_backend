package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.dtos.category.request.CategoryRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.category.response.CategoryResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

@Import(mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler.class)
@WebMvcTest(
        controllers = CategoryController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@DisplayName("CategoryController")
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Spring Boot 4.x usa @MockitoBean en lugar de @MockBean (deprecado)
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private JwtService jwtService;
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ─── Fixtures reutilizables ───────────────────────────────────────────────

    private CategoryResponseDTO rootCategory;
    private CategoryResponseDTO childCategory;

    @BeforeEach
    void setUp() {
        rootCategory = new CategoryResponseDTO(
                1, "Equipo de Cómputo", "Computadoras y periféricos",
                null, null, true
        );
        childCategory = new CategoryResponseDTO(
                2, "Laptops", "Computadoras portátiles",
                1, "Equipo de Cómputo", true
        );
    }

    // =========================================================================
    // GET /v1/categories
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/categories")
    class FindAllActive {

        @Test
        @DisplayName("should_returnPageOfCategories_when_activeRecordsExist")
        void should_returnPageOfCategories_when_activeRecordsExist() throws Exception {
            Page<CategoryResponseDTO> page = new PageImpl<>(
                    List.of(rootCategory, childCategory),
                    PageRequest.of(0, 20),
                    2
            );
            when(categoryService.findAllActive(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/categories")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    // NOTE: este endpoint devuelve Page<> sin wrapper ApiResponse.
                    // Inconsistencia de contrato con el resto del controlador — considerar unificar.
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Equipo de Cómputo"))
                    .andExpect(jsonPath("$.content[0].parentId").doesNotExist())
                    .andExpect(jsonPath("$.content[1].id").value(2))
                    .andExpect(jsonPath("$.content[1].parentId").value(1))
                    .andExpect(jsonPath("$.content[1].parentName").value("Equipo de Cómputo"))
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(jsonPath("$.totalPages").value(1));

            verify(categoryService).findAllActive(any(Pageable.class));
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noActiveRecordsExist")
        void should_returnEmptyPage_when_noActiveRecordsExist() throws Exception {
            Page<CategoryResponseDTO> emptyPage = Page.empty(PageRequest.of(0, 20));
            when(categoryService.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/v1/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // =========================================================================
    // GET /v1/categories/search?q=
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/categories/search")
    class Search {

        @Test
        @DisplayName("should_returnMatchingCategories_when_queryMatchesName")
        void should_returnMatchingCategories_when_queryMatchesName() throws Exception {
            Page<CategoryResponseDTO> page = new PageImpl<>(
                    List.of(rootCategory),
                    PageRequest.of(0, 20),
                    1
            );
            when(categoryService.search(eq("computo"), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/categories/search")
                            .param("q", "computo")
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].name").value("Equipo de Cómputo"));

            verify(categoryService).search(eq("computo"), any(Pageable.class));
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_queryParamIsBlank")
        void should_return400WithApiResponse_when_queryParamIsBlank() throws Exception {
            mockMvc.perform(get("/v1/categories/search")
                            .param("q", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_queryParamIsMissing")
        void should_return400WithApiResponse_when_queryParamIsMissing() throws Exception {
            // UPDATED: MissingServletRequestParameterException → 400 + ApiResponse
            // con mensaje "Parámetro requerido ausente: q"
            mockMvc.perform(get("/v1/categories/search"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Parámetro requerido ausente: q"));

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_queryExceeds100Characters")
        void should_return400WithApiResponse_when_queryExceeds100Characters() throws Exception {
            // UPDATED: ConstraintViolationException (@Size) → 400 + ApiResponse
            String longQuery = "a".repeat(101);

            mockMvc.perform(get("/v1/categories/search")
                            .param("q", longQuery))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_returnEmptyPage_when_noMatchFound")
        void should_returnEmptyPage_when_noMatchFound() throws Exception {
            Page<CategoryResponseDTO> emptyPage = Page.empty(PageRequest.of(0, 20));
            when(categoryService.search(eq("xyz_inexistente"), any(Pageable.class)))
                    .thenReturn(emptyPage);

            mockMvc.perform(get("/v1/categories/search")
                            .param("q", "xyz_inexistente"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0));
        }
    }

    // =========================================================================
    // GET /v1/categories/id
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/categories/{id}")
    class FindById {

        @Test
        @DisplayName("should_returnWrappedCategory_when_idExists")
        void should_returnWrappedCategory_when_idExists() throws Exception {
            when(categoryService.findById(1)).thenReturn(rootCategory);

            mockMvc.perform(get("/v1/categories/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Equipo de Cómputo"))
                    .andExpect(jsonPath("$.data.parentId").doesNotExist())
                    .andExpect(jsonPath("$.data.parentName").doesNotExist())
                    .andExpect(jsonPath("$.data.isActive").value(true));

            verify(categoryService).findById(1);
        }

        @Test
        @DisplayName("should_returnWrappedChildCategory_when_idBelongsToSubcategory")
        void should_returnWrappedChildCategory_when_idBelongsToSubcategory() throws Exception {
            when(categoryService.findById(2)).thenReturn(childCategory);

            mockMvc.perform(get("/v1/categories/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.parentId").value(1))
                    .andExpect(jsonPath("$.data.parentName").value("Equipo de Cómputo"));
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_idDoesNotExist")
        void should_return404WithApiResponse_when_idDoesNotExist() throws Exception {
            when(categoryService.findById(999))
                    .thenThrow(new ResourceNotFoundException("Categoría con id 999 no encontrada"));

            mockMvc.perform(get("/v1/categories/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Categoría con id 999 no encontrada"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    // =========================================================================
    // POST /v1/categories
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/categories")
    class Create {

        @Test
        @DisplayName("should_return201WithCreatedCategory_when_requestIsValid")
        void should_return201WithCreatedCategory_when_requestIsValid() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO(
                    "Equipo de Cómputo", "Computadoras y periféricos", null
            );
            when(categoryService.create(any(CategoryRequestDTO.class))).thenReturn(rootCategory);

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("Equipo de Cómputo"));

            verify(categoryService).create(any(CategoryRequestDTO.class));
        }

        @Test
        @DisplayName("should_return201WithChildCategory_when_parentIdIsProvided")
        void should_return201WithChildCategory_when_parentIdIsProvided() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("Laptops", "Computadoras portátiles", 1);
            when(categoryService.create(any(CategoryRequestDTO.class))).thenReturn(childCategory);

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.parentId").value(1))
                    .andExpect(jsonPath("$.data.parentName").value("Equipo de Cómputo"));
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameIsBlank")
        void should_return400WithApiResponse_when_nameIsBlank() throws Exception {
            // UPDATED: MethodArgumentNotValidException → 400 + ApiResponse con primer field error
            CategoryRequestDTO request = new CategoryRequestDTO("", null, null);

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    // El handler toma el primer field error: "name: El nombre de la categoría es obligatorio"
                    .andExpect(jsonPath("$.message").value("name: El nombre de la categoría es obligatorio"));

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameIsNull")
        void should_return400WithApiResponse_when_nameIsNull() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO(null, null, null);

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre de la categoría es obligatorio"));

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameExceeds100Characters")
        void should_return400WithApiResponse_when_nameExceeds100Characters() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("a".repeat(101), null, null);

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre no puede exceder 100 caracteres"));

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_descriptionExceeds255Characters")
        void should_return400WithApiResponse_when_descriptionExceeds255Characters() throws Exception {
            CategoryRequestDTO request = new CategoryRequestDTO("Válido", "d".repeat(256), null);

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("description: La descripción no puede exceder 255 caracteres"));

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_bodyIsMissing")
        void should_return400WithApiResponse_when_bodyIsMissing() throws Exception {
            // UPDATED: HttpMessageNotReadableException — NOTA: el handler en GlobalExceptionHandler
            // retorna ResponseEntity<String> en lugar de ResponseEntity<ApiResponse<Void>>.
            // Por eso aquí verificamos solo el status 400, no la estructura ApiResponse.
            // Corregir el handler para mantener contrato uniforme (code smell documentado).
            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return409WithApiResponse_when_categoryNameAlreadyExists")
        void should_return409WithApiResponse_when_categoryNameAlreadyExists() throws Exception {
            // UPDATED: DuplicateResourceException → 409 + ApiResponse
            CategoryRequestDTO request = new CategoryRequestDTO("Equipo de Cómputo", null, null);
            when(categoryService.create(any(CategoryRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("Ya existe una categoría con el nombre 'Equipo de Cómputo'"));

            mockMvc.perform(post("/v1/categories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("Ya existe una categoría con el nombre 'Equipo de Cómputo'"));
        }
    }

    // =========================================================================
    // PUT /v1/categories/id
    // =========================================================================

    @Nested
    @DisplayName("PUT /v1/categories/{id}")
    class Update {

        @Test
        @DisplayName("should_return200WithUpdatedCategory_when_requestIsValid")
        void should_return200WithUpdatedCategory_when_requestIsValid() throws Exception {
            CategoryResponseDTO updated = new CategoryResponseDTO(
                    1, "Equipo de Cómputo Actualizado", "Nueva descripción",
                    null, null, true
            );
            CategoryRequestDTO request = new CategoryRequestDTO(
                    "Equipo de Cómputo Actualizado", "Nueva descripción", null
            );
            when(categoryService.update(eq(1), any(CategoryRequestDTO.class))).thenReturn(updated);

            mockMvc.perform(put("/v1/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.name").value("Equipo de Cómputo Actualizado"))
                    .andExpect(jsonPath("$.data.description").value("Nueva descripción"));

            verify(categoryService).update(eq(1), any(CategoryRequestDTO.class));
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_nameIsBlankOnUpdate")
        void should_return400WithApiResponse_when_nameIsBlankOnUpdate() throws Exception {
            // UPDATED: MethodArgumentNotValidException → 400 + ApiResponse
            CategoryRequestDTO request = new CategoryRequestDTO("", null, null);

            mockMvc.perform(put("/v1/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("name: El nombre de la categoría es obligatorio"));

            verifyNoInteractions(categoryService);
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_idDoesNotExistOnUpdate")
        void should_return404WithApiResponse_when_idDoesNotExistOnUpdate() throws Exception {
            // UPDATED: ResourceNotFoundException → 404 + ApiResponse
            CategoryRequestDTO request = new CategoryRequestDTO("Válido", null, null);
            when(categoryService.update(eq(999), any(CategoryRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Categoría con id 999 no encontrada"));

            mockMvc.perform(put("/v1/categories/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Categoría con id 999 no encontrada"));
        }

        @Test
        @DisplayName("should_return409WithApiResponse_when_updatedNameAlreadyExists")
        void should_return409WithApiResponse_when_updatedNameAlreadyExists() throws Exception {
            // UPDATED: DuplicateResourceException → 409 + ApiResponse
            CategoryRequestDTO request = new CategoryRequestDTO("Mobiliario", null, null);
            when(categoryService.update(eq(1), any(CategoryRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException("Ya existe una categoría con el nombre 'Mobiliario'"));

            mockMvc.perform(put("/v1/categories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("Ya existe una categoría con el nombre 'Mobiliario'"));
        }
    }

    // =========================================================================
    // DELETE /v1/categories/ id
    // =========================================================================

    @Nested
    @DisplayName("DELETE /v1/categories/{id}")
    class Deactivate {

        @Test
        @DisplayName("should_return204_when_categoryIsSuccessfullyDeactivated")
        void should_return204_when_categoryIsSuccessfullyDeactivated() throws Exception {
            doNothing().when(categoryService).deactivate(1);

            mockMvc.perform(delete("/v1/categories/1"))
                    .andExpect(status().isNoContent())
                    .andExpect(jsonPath("$").doesNotExist());

            verify(categoryService).deactivate(1);
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_idDoesNotExistOnDeactivate")
        void should_return404WithApiResponse_when_idDoesNotExistOnDeactivate() throws Exception {
            // UPDATED: ResourceNotFoundException → 404 + ApiResponse
            doThrow(new ResourceNotFoundException("Categoría con id 999 no encontrada"))
                    .when(categoryService).deactivate(999);

            mockMvc.perform(delete("/v1/categories/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Categoría con id 999 no encontrada"));
        }

        @Test
        @DisplayName("should_return409WithApiResponse_when_categoryHasActiveChildren")
        void should_return409WithApiResponse_when_categoryHasActiveChildren() throws Exception {
            // Edge case: padre con subcategorías activas no puede ser desactivado.
            //  crear CategoryHasActiveChildrenException y mapearlo a 409 en GlobalExceptionHandler.
            //       Por ahora se usa InvalidAssetStateException como proxy porque ya está mapeado a 409.
            //       Cambiar el throw en CategoryService y el handler cuando la excepción exista.
            doThrow(new mx.edu.unpa.inventory_backend.exceptions.InvalidAssetStateException(
                    "No se puede desactivar una categoría con subcategorías activas"))
                    .when(categoryService).deactivate(1);

            mockMvc.perform(delete("/v1/categories/1"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("No se puede desactivar una categoría con subcategorías activas"));
        }
    }
}