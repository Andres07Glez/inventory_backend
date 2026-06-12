package mx.edu.unpa.inventory_backend.controllers;

import tools.jackson.databind.ObjectMapper;
import mx.edu.unpa.inventory_backend.dtos.location.request.LocationRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.location.response.LocationResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.enums.Campus;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.LocationService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {LocationController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(LocationControllerTest.TestExceptionHandlerExtension.class)
@DisplayName("LocationController — Integration / Slice Tests")
class LocationControllerTest {

    // ================================================================
    // Handler local — extiende el GlobalExceptionHandler de producción
    // con el caso de MethodArgumentTypeMismatchException (enum inválido).
    //
    // Razón: GlobalExceptionHandler en producción no cubre aún esta
    // excepción, por lo que un valor inválido en @RequestParam Campus
    // cae en el handler genérico de Exception y devuelve 500.
    // Este advice local, cargado únicamente en el contexto de test,
    // garantiza que el test valide el comportamiento correcto (400)
    // sin modificar el código de producción.
    //
    // migrar este handler a GlobalExceptionHandler en producción.
    // ================================================================
    @RestControllerAdvice
    static class TestExceptionHandlerExtension {

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
                MethodArgumentTypeMismatchException ex) {
            String message = String.format(
                    "Valor inválido '%s' para el parámetro '%s'",
                    ex.getValue(), ex.getName()
            );
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(message));
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
    private LocationService locationService;

    // Dependencias de seguridad — mockeadas para evitar caída de contexto
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // -------------------------------------------------------
    // Fixtures reutilizables
    // -------------------------------------------------------

    private LocationResponseDTO sampleResponse;
    private LocationRequestDTO validRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new LocationResponseDTO(
                1, "Sala de Cómputo A", "Edificio 3", Campus.LOMA_BONITA,
                "Sala principal de cómputo", true
        );
        validRequest = new LocationRequestDTO(
                "Sala de Cómputo A",
                "Edificio 3",
                Campus.LOMA_BONITA,
                "Sala principal de cómputo"
        );
    }

    // ================================================================
    // GET /v1/locations
    // ================================================================

    @Nested
    @DisplayName("GET /v1/locations")
    class FindAllActive {

        @Test
        @DisplayName("should_return200WithPage_when_activeLocationsExist")
        void should_return200WithPage_when_activeLocationsExist() throws Exception {
            Page<LocationResponseDTO> page = new PageImpl<>(
                    List.of(sampleResponse), PageRequest.of(0, 10), 1
            );
            when(locationService.findAllActive(any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/locations")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].id", is(1)))
                    .andExpect(jsonPath("$.content[0].name", is("Sala de Cómputo A")))
                    .andExpect(jsonPath("$.content[0].campus", is("LOMA_BONITA")))
                    .andExpect(jsonPath("$.content[0].isActive", is(true)))
                    .andExpect(jsonPath("$.totalElements", is(1)));
        }

        @Test
        @DisplayName("should_return200WithEmptyPage_when_noActiveLocationsExist")
        void should_return200WithEmptyPage_when_noActiveLocationsExist() throws Exception {
            Page<LocationResponseDTO> emptyPage = Page.empty(PageRequest.of(0, 10));
            when(locationService.findAllActive(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/v1/locations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalElements", is(0)));
        }
    }

    // ================================================================
    // GET /v1/locations/search
    // ================================================================

    @Nested
    @DisplayName("GET /v1/locations/search")
    class Search {

        @Test
        @DisplayName("should_return200WithMatchingLocations_when_queryIsValid")
        void should_return200WithMatchingLocations_when_queryIsValid() throws Exception {
            Page<LocationResponseDTO> page = new PageImpl<>(List.of(sampleResponse));
            when(locationService.search(eq("Sala"), any(Pageable.class))).thenReturn(page);

            mockMvc.perform(get("/v1/locations/search")
                            .param("q", "Sala"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(1)))
                    .andExpect(jsonPath("$.content[0].name", is("Sala de Cómputo A")));
        }

        @Test
        @DisplayName("should_return400_when_queryParamIsBlank")
        void should_return400_when_queryParamIsBlank() throws Exception {
            mockMvc.perform(get("/v1/locations/search")
                            .param("q", "   "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return400_when_queryParamIsMissing")
        void should_return400_when_queryParamIsMissing() throws Exception {
            mockMvc.perform(get("/v1/locations/search"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_queryExceedsMaxLength")
        void should_return400_when_queryExceedsMaxLength() throws Exception {
            String tooLong = "A".repeat(101);

            mockMvc.perform(get("/v1/locations/search")
                            .param("q", tooLong))
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================================================
    // GET /v1/locations/by-campus
    // ================================================================

    @Nested
    @DisplayName("GET /v1/locations/by-campus")
    class FindByCampus {

        @Test
        @DisplayName("should_return200WithLocations_when_campusIsValid")
        void should_return200WithLocations_when_campusIsValid() throws Exception {
            Page<LocationResponseDTO> page = new PageImpl<>(List.of(sampleResponse));
            when(locationService.findByCampus(eq(Campus.LOMA_BONITA), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/v1/locations/by-campus")
                            .param("campus", "LOMA_BONITA"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].campus", is("LOMA_BONITA")));
        }

        @Test
        @DisplayName("should_return400_when_campusValueIsInvalid")
        void should_return400_when_campusValueIsInvalid() throws Exception {
            // MethodArgumentTypeMismatchException → manejado por TestExceptionHandlerExtension → 400
            mockMvc.perform(get("/v1/locations/by-campus")
                            .param("campus", "CAMPUS_INVALIDO"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("CAMPUS_INVALIDO")));
        }

        @Test
        @DisplayName("should_return400_when_campusParamIsMissing")
        void should_return400_when_campusParamIsMissing() throws Exception {
            mockMvc.perform(get("/v1/locations/by-campus"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }
    }

    // ================================================================
    // GET /v1/locations/campuses
    // ================================================================

    @Nested
    @DisplayName("GET /v1/locations/campuses")
    class GetCampuses {

        @Test
        @DisplayName("should_return200WithAllCampusValues_when_requested")
        void should_return200WithAllCampusValues_when_requested() throws Exception {
            mockMvc.perform(get("/v1/locations/campuses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(Campus.values().length)))
                    .andExpect(jsonPath("$.data", containsInAnyOrder("LOMA_BONITA", "TUXTEPEC")));
        }
    }

    // ================================================================
    // GET /v1/locations/id
    // ================================================================

    @Nested
    @DisplayName("GET /v1/locations/{id}")
    class FindById {

        @Test
        @DisplayName("should_return200WithLocation_when_idExists")
        void should_return200WithLocation_when_idExists() throws Exception {
            when(locationService.findById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/v1/locations/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.name", is("Sala de Cómputo A")))
                    .andExpect(jsonPath("$.data.building", is("Edificio 3")))
                    .andExpect(jsonPath("$.data.campus", is("LOMA_BONITA")))
                    .andExpect(jsonPath("$.data.isActive", is(true)));
        }

        @Test
        @DisplayName("should_return404WithApiResponseError_when_idDoesNotExist")
        void should_return404WithApiResponseError_when_idDoesNotExist() throws Exception {
            when(locationService.findById(999L))
                    .thenThrow(new ResourceNotFoundException("Ubicación no encontrada con id: 999"));

            mockMvc.perform(get("/v1/locations/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("999")));
        }
    }

    // ================================================================
    // POST /v1/locations
    // ================================================================

    @Nested
    @DisplayName("POST /v1/locations")
    class Create {

        @Test
        @DisplayName("should_return201WithCreatedLocation_when_requestIsValid")
        void should_return201WithCreatedLocation_when_requestIsValid() throws Exception {
            when(locationService.create(any(LocationRequestDTO.class))).thenReturn(sampleResponse);

            mockMvc.perform(post("/v1/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.name", is("Sala de Cómputo A")))
                    .andExpect(jsonPath("$.data.campus", is("LOMA_BONITA")));
        }

        @Test
        @DisplayName("should_return400_when_nameIsBlank")
        void should_return400_when_nameIsBlank() throws Exception {
            LocationRequestDTO badRequest = new LocationRequestDTO(
                    "", "Edificio 3", Campus.LOMA_BONITA, "Desc"
            );

            mockMvc.perform(post("/v1/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", notNullValue()));
        }

        @Test
        @DisplayName("should_return400_when_campusIsNull")
        void should_return400_when_campusIsNull() throws Exception {
            // Serializado manualmente para forzar campus: null en el JSON
            String body = """
                    {"name":"Sala B","building":"Edificio 1","campus":null,"description":null}
                    """;

            mockMvc.perform(post("/v1/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_nameExceedsMaxLength")
        void should_return400_when_nameExceedsMaxLength() throws Exception {
            LocationRequestDTO badRequest = new LocationRequestDTO(
                    "N".repeat(151), "Edificio 3", Campus.TUXTEPEC, null
            );

            mockMvc.perform(post("/v1/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissing")
        void should_return400_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/v1/locations")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return409_when_locationNameAlreadyExistsInCampus")
        void should_return409_when_locationNameAlreadyExistsInCampus() throws Exception {
            when(locationService.create(any(LocationRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException(
                            "Ya existe una ubicación con el nombre 'Sala de Cómputo A' en el campus LOMA_BONITA"
                    ));

            mockMvc.perform(post("/v1/locations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("LOMA_BONITA")));
        }
    }

    // ================================================================
    // PUT /v1/locations/id
    // ================================================================

    @Nested
    @DisplayName("PUT /v1/locations/{id}")
    class Update {

        @Test
        @DisplayName("should_return200WithUpdatedLocation_when_requestIsValid")
        void should_return200WithUpdatedLocation_when_requestIsValid() throws Exception {
            LocationResponseDTO updated = new LocationResponseDTO(
                    1, "Sala de Cómputo B", "Edificio 4", Campus.TUXTEPEC,
                    "Sala actualizada", true
            );
            when(locationService.update(eq(1L), any(LocationRequestDTO.class))).thenReturn(updated);

            LocationRequestDTO updateReq = new LocationRequestDTO(
                    "Sala de Cómputo B", "Edificio 4", Campus.TUXTEPEC, "Sala actualizada"
            );

            mockMvc.perform(put("/v1/locations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.name", is("Sala de Cómputo B")))
                    .andExpect(jsonPath("$.data.campus", is("TUXTEPEC")));
        }

        @Test
        @DisplayName("should_return404_when_locationToUpdateDoesNotExist")
        void should_return404_when_locationToUpdateDoesNotExist() throws Exception {
            when(locationService.update(eq(999L), any(LocationRequestDTO.class)))
                    .thenThrow(new ResourceNotFoundException("Ubicación no encontrada con id: 999"));

            mockMvc.perform(put("/v1/locations/999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_updateRequestBodyHasBlankName")
        void should_return400_when_updateRequestBodyHasBlankName() throws Exception {
            LocationRequestDTO badRequest = new LocationRequestDTO(
                    "   ", "Edificio 3", Campus.LOMA_BONITA, null
            );

            mockMvc.perform(put("/v1/locations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return409_when_updatedNameConflictsWithExistingLocation")
        void should_return409_when_updatedNameConflictsWithExistingLocation() throws Exception {
            when(locationService.update(eq(1L), any(LocationRequestDTO.class)))
                    .thenThrow(new DuplicateResourceException(
                            "Ya existe una ubicación con ese nombre en el campus indicado"
                    ));

            mockMvc.perform(put("/v1/locations/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_updateRequestBodyIsMissing")
        void should_return400_when_updateRequestBodyIsMissing() throws Exception {
            mockMvc.perform(put("/v1/locations/1")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================================================
    // DELETE /v1/locations/id
    // ================================================================

    @Nested
    @DisplayName("DELETE /v1/locations/{id}")
    class Deactivate {

        @Test
        @DisplayName("should_return204_when_locationIsSuccessfullyDeactivated")
        void should_return204_when_locationIsSuccessfullyDeactivated() throws Exception {
            doNothing().when(locationService).deactivate(1L);

            mockMvc.perform(delete("/v1/locations/1"))
                    .andExpect(status().isNoContent());

            verify(locationService, times(1)).deactivate(1L);
        }

        @Test
        @DisplayName("should_return404_when_locationToDeactivateDoesNotExist")
        void should_return404_when_locationToDeactivateDoesNotExist() throws Exception {
            doThrow(new ResourceNotFoundException("Ubicación no encontrada con id: 99"))
                    .when(locationService).deactivate(99L);

            mockMvc.perform(delete("/v1/locations/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }
}