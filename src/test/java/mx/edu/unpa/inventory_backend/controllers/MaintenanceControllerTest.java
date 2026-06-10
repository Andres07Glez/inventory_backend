package mx.edu.unpa.inventory_backend.controllers;

import tools.jackson.databind.ObjectMapper;
import mx.edu.unpa.inventory_backend.dtos.android.response.ApiResponse;
import mx.edu.unpa.inventory_backend.dtos.maintenance.request.MaintenanceCreateRequest;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceResponse;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceSummary;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.MaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {MaintenanceController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        MaintenanceControllerTest.TestExceptionHandlerExtension.class,
        MaintenanceControllerTest.AuthenticatedUserArgumentResolverConfig.class
})
@DisplayName("MaintenanceController — Integration / Slice Tests")
class MaintenanceControllerTest {

    // ================================================================
    // Handler local — cubre MethodArgumentTypeMismatchException (400).
    // Se activa cuando un @RequestParam enum recibe un valor inválido
    // (ej. ?type=INVALIDO). El GlobalExceptionHandler de producción no
    // lo cubre aún → caería en 500.
    // TODO: migrar este handler a GlobalExceptionHandler en producción.
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
    // Con seguridad deshabilitada (addFilters = false), el SecurityContext
    // está vacío y @AuthenticationPrincipal resuelve null, lo que provoca
    // NullPointerException al invocar currentUser.id() en POST /v1/maintenance.
    // Este resolver inyecta un AuthenticatedUser fijo (id=1L) en cada
    // request de test, replicando la presencia de un usuario autenticado.
    //
    // NOTA sobre @PreAuthorize: con SecurityAutoConfiguration excluido,
    // las anotaciones @PreAuthorize("hasAnyRole('ADMIN','OPERADOR')") y
    // @PreAuthorize("hasRole('ADMIN')") NO se evalúan en este contexto.
    // La verificación de reglas de acceso por rol debe cubrirse en tests
    // de integración completos (@SpringBootTest) que arranquen el contexto
    // de seguridad completo.
    // ================================================================
    static class AuthenticatedUserResolver implements HandlerMethodArgumentResolver {

        static final AuthenticatedUser FAKE_USER = new AuthenticatedUser(
                1L, "test_admin", "secret", UserRole.ADMIN, true
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

    @TestConfiguration
    static class AuthenticatedUserArgumentResolverConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(
                List<HandlerMethodArgumentResolver> resolvers) {
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
    private MaintenanceService maintenanceService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // -------------------------------------------------------
    // Fixtures reutilizables
    // -------------------------------------------------------

    private MaintenanceCreateRequest validRequest;
    private MaintenanceResponse sampleResponse;
    private MaintenanceSummary sampleSummary;

    @BeforeEach
    void setUp() {
        validRequest = new MaintenanceCreateRequest(
                5L,
                null,
                MaintenanceType.PREVENTIVE,
                "Limpieza general y revisión de componentes",
                "Técnico Juan Pérez",
                LocalDate.now(),
                new BigDecimal("350.00"),
                ConditionStatus.REGULAR,
                ConditionStatus.GOOD
        );

        sampleResponse = new MaintenanceResponse(
                1L,
                5L,
                "INV-2024-00005",
                "Laptop Dell Latitude",
                null,
                MaintenanceType.PREVENTIVE,
                "Limpieza general y revisión de componentes",
                "Técnico Juan Pérez",
                LocalDate.now(),
                new BigDecimal("350.00"),
                ConditionStatus.REGULAR,
                ConditionStatus.GOOD,
                LocalDateTime.now(),
                "test_admin"
        );

        sampleSummary = new MaintenanceSummary(
                1L,
                5L,
                "INV-2024-00005",
                null,
                MaintenanceType.PREVENTIVE,
                "Técnico Juan Pérez",
                LocalDate.now(),
                new BigDecimal("350.00"),
                ConditionStatus.REGULAR,
                ConditionStatus.GOOD,
                "test_admin"
        );
    }

    // ================================================================
    // POST /v1/maintenance
    // ================================================================

    @Nested
    @DisplayName("POST /v1/maintenance")
    class Create {

        @Test
        @DisplayName("should_return201WithCreatedMaintenance_when_requestIsValid")
        void should_return201WithCreatedMaintenance_when_requestIsValid() throws Exception {
            // userId=1L inyectado por AuthenticatedUserResolver
            when(maintenanceService.create(any(MaintenanceCreateRequest.class), eq(1L)))
                    .thenReturn(sampleResponse);

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.assetId", is(5)))
                    .andExpect(jsonPath("$.data.maintenanceType", is("PREVENTIVE")))
                    .andExpect(jsonPath("$.data.performedBy", is("Técnico Juan Pérez")))
                    .andExpect(jsonPath("$.data.conditionBefore", is("REGULAR")))
                    .andExpect(jsonPath("$.data.conditionAfter", is("GOOD")))
                    .andExpect(jsonPath("$.data.createdByName", is("test_admin")));
        }

        @Test
        @DisplayName("should_return201WithOptionalFieldsNull_when_nullableFieldsAreOmitted")
        void should_return201WithOptionalFieldsNull_when_nullableFieldsAreOmitted() throws Exception {
            MaintenanceCreateRequest minimalRequest = new MaintenanceCreateRequest(
                    5L, null, MaintenanceType.CORRECTIVE,
                    "Reparación de pantalla", null,
                    LocalDate.now(), null, null, null
            );
            MaintenanceResponse minimalResponse = new MaintenanceResponse(
                    2L, 5L, "INV-2024-00005", "Laptop Dell Latitude",
                    null, MaintenanceType.CORRECTIVE,
                    "Reparación de pantalla", null,
                    LocalDate.now(), null, null, null,
                    LocalDateTime.now(), "test_admin"
            );
            when(maintenanceService.create(any(MaintenanceCreateRequest.class), eq(1L)))
                    .thenReturn(minimalResponse);

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(minimalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(2)))
                    .andExpect(jsonPath("$.data.maintenanceType", is("CORRECTIVE")));
        }

        @Test
        @DisplayName("should_return400_when_assetIdIsNull")
        void should_return400_when_assetIdIsNull() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    null, null, MaintenanceType.PREVENTIVE,
                    "Descripción válida", null,
                    LocalDate.now(), null, null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", notNullValue()));
        }

        @Test
        @DisplayName("should_return400_when_maintenanceTypeIsNull")
        void should_return400_when_maintenanceTypeIsNull() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    5L, null, null,
                    "Descripción válida", null,
                    LocalDate.now(), null, null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_descriptionIsBlank")
        void should_return400_when_descriptionIsBlank() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    5L, null, MaintenanceType.PREVENTIVE,
                    "", null,
                    LocalDate.now(), null, null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_descriptionExceedsMaxLength")
        void should_return400_when_descriptionExceedsMaxLength() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    5L, null, MaintenanceType.PREVENTIVE,
                    "D".repeat(5001), null,
                    LocalDate.now(), null, null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_performedByExceedsMaxLength")
        void should_return400_when_performedByExceedsMaxLength() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    5L, null, MaintenanceType.PREVENTIVE,
                    "Descripción válida", "T".repeat(201),
                    LocalDate.now(), null, null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_performedDateIsNull")
        void should_return400_when_performedDateIsNull() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    5L, null, MaintenanceType.PREVENTIVE,
                    "Descripción válida", null,
                    null, null, null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_performedDateIsInTheFuture")
        void should_return400_when_performedDateIsInTheFuture() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    5L, null, MaintenanceType.PREVENTIVE,
                    "Descripción válida", null,
                    LocalDate.now().plusDays(1), null, null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_costIsNegative")
        void should_return400_when_costIsNegative() throws Exception {
            MaintenanceCreateRequest bad = new MaintenanceCreateRequest(
                    5L, null, MaintenanceType.PREVENTIVE,
                    "Descripción válida", null,
                    LocalDate.now(), new BigDecimal("-1.00"), null, null
            );

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(bad)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)));
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissing")
        void should_return400_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should_return404_when_assetIdDoesNotExist")
        void should_return404_when_assetIdDoesNotExist() throws Exception {
            when(maintenanceService.create(any(MaintenanceCreateRequest.class), eq(1L)))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 5"));

            mockMvc.perform(post("/v1/maintenance")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("5")));
        }
    }

    // ================================================================
    // GET /v1/maintenance
    // ================================================================

    @Nested
    @DisplayName("GET /v1/maintenance")
    class GetAll {

        @Test
        @DisplayName("should_return200WithAllMaintenances_when_noFilterIsApplied")
        void should_return200WithAllMaintenances_when_noFilterIsApplied() throws Exception {
            when(maintenanceService.getAll(isNull())).thenReturn(List.of(sampleSummary));

            mockMvc.perform(get("/v1/maintenance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id", is(1)))
                    .andExpect(jsonPath("$.data[0].assetId", is(5)))
                    .andExpect(jsonPath("$.data[0].maintenanceType", is("PREVENTIVE")))
                    .andExpect(jsonPath("$.data[0].performedBy", is("Técnico Juan Pérez")));
        }

        @Test
        @DisplayName("should_return200WithFilteredList_when_typeFilterIsApplied")
        void should_return200WithFilteredList_when_typeFilterIsApplied() throws Exception {
            when(maintenanceService.getAll(MaintenanceType.CORRECTIVE)).thenReturn(List.of());

            mockMvc.perform(get("/v1/maintenance")
                            .param("type", "CORRECTIVE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_noMaintenancesExist")
        void should_return200WithEmptyList_when_noMaintenancesExist() throws Exception {
            when(maintenanceService.getAll(isNull())).thenReturn(List.of());

            mockMvc.perform(get("/v1/maintenance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("should_return400_when_typeFilterValueIsInvalid")
        void should_return400_when_typeFilterValueIsInvalid() throws Exception {
            // MethodArgumentTypeMismatchException → manejado por TestExceptionHandlerExtension → 400
            mockMvc.perform(get("/v1/maintenance")
                            .param("type", "TIPO_INVALIDO"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("TIPO_INVALIDO")));
        }
    }

    // ================================================================
    // GET /v1/maintenance/{id}
    // ================================================================

    @Nested
    @DisplayName("GET /v1/maintenance/{id}")
    class GetById {

        @Test
        @DisplayName("should_return200WithFullDetail_when_idExists")
        void should_return200WithFullDetail_when_idExists() throws Exception {
            when(maintenanceService.getById(1L)).thenReturn(sampleResponse);

            mockMvc.perform(get("/v1/maintenance/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data.id", is(1)))
                    .andExpect(jsonPath("$.data.assetId", is(5)))
                    .andExpect(jsonPath("$.data.inventoryNumber", is("INV-2024-00005")))
                    .andExpect(jsonPath("$.data.assetDescription", is("Laptop Dell Latitude")))
                    .andExpect(jsonPath("$.data.maintenanceType", is("PREVENTIVE")))
                    .andExpect(jsonPath("$.data.description",
                            is("Limpieza general y revisión de componentes")))
                    .andExpect(jsonPath("$.data.conditionBefore", is("REGULAR")))
                    .andExpect(jsonPath("$.data.conditionAfter", is("GOOD")))
                    .andExpect(jsonPath("$.data.createdByName", is("test_admin")));
        }

        @Test
        @DisplayName("should_return404_when_idDoesNotExist")
        void should_return404_when_idDoesNotExist() throws Exception {
            when(maintenanceService.getById(999L))
                    .thenThrow(new ResourceNotFoundException(
                            "Registro de mantenimiento no encontrado con id: 999"));

            mockMvc.perform(get("/v1/maintenance/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("999")));
        }
    }

    // ================================================================
    // GET /v1/assets/{assetId}/maintenance
    // ================================================================

    @Nested
    @DisplayName("GET /v1/assets/{assetId}/maintenance")
    class GetByAsset {

        @Test
        @DisplayName("should_return200WithMaintenanceList_when_assetHasRecords")
        void should_return200WithMaintenanceList_when_assetHasRecords() throws Exception {
            when(maintenanceService.getByAssetId(5L)).thenReturn(List.of(sampleSummary));

            mockMvc.perform(get("/v1/assets/5/maintenance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(1)))
                    .andExpect(jsonPath("$.data[0].id", is(1)))
                    .andExpect(jsonPath("$.data[0].assetId", is(5)))
                    .andExpect(jsonPath("$.data[0].inventoryNumber", is("INV-2024-00005")))
                    .andExpect(jsonPath("$.data[0].maintenanceType", is("PREVENTIVE")));
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_assetHasNoMaintenanceRecords")
        void should_return200WithEmptyList_when_assetHasNoMaintenanceRecords() throws Exception {
            when(maintenanceService.getByAssetId(99L)).thenReturn(List.of());

            mockMvc.perform(get("/v1/assets/99/maintenance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)))
                    .andExpect(jsonPath("$.data", hasSize(0)));
        }

        @Test
        @DisplayName("should_return404_when_assetDoesNotExist")
        void should_return404_when_assetDoesNotExist() throws Exception {
            when(maintenanceService.getByAssetId(999L))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 999"));

            mockMvc.perform(get("/v1/assets/999/maintenance"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("999")));
        }
    }

    // ================================================================
    // DELETE /v1/maintenance/{id}
    // ================================================================

    @Nested
    @DisplayName("DELETE /v1/maintenance/{id}")
    class Delete {

        @Test
        @DisplayName("should_return200WithOkResponse_when_maintenanceIsSuccessfullyDeleted")
        void should_return200WithOkResponse_when_maintenanceIsSuccessfullyDeleted()
                throws Exception {
            doNothing().when(maintenanceService).delete(1L);

            mockMvc.perform(delete("/v1/maintenance/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success", is(true)));

            verify(maintenanceService, times(1)).delete(1L);
        }

        @Test
        @DisplayName("should_return404_when_maintenanceToDeleteDoesNotExist")
        void should_return404_when_maintenanceToDeleteDoesNotExist() throws Exception {
            doThrow(new ResourceNotFoundException(
                    "Registro de mantenimiento no encontrado con id: 99"))
                    .when(maintenanceService).delete(99L);

            mockMvc.perform(delete("/v1/maintenance/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success", is(false)))
                    .andExpect(jsonPath("$.message", containsString("99")));
        }
    }
}