package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.request.UpdateConditionRequest;
import mx.edu.unpa.inventory_backend.dtos.asset.response.*;
import mx.edu.unpa.inventory_backend.dtos.asset_assignment.response.AssignmentHistoryResponse;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianSummary;
import mx.edu.unpa.inventory_backend.enums.Campus;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.AssetQueryService;
import mx.edu.unpa.inventory_backend.services.AssetService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({GlobalExceptionHandler.class, AssetControllerTest.TestWebConfig.class})
@WebMvcTest(
        controllers = AssetController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AssetController")
class AssetControllerTest {

    private static final String BASE_URL = "/v1/assets";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean private AssetService assetService;
    @MockitoBean private AssetQueryService assetQueryService;
    @MockitoBean private AssetRepository assetRepository;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private RequestPostProcessor principalPostProcessor;

    // Responses
    private AssetResponseDTO assetResponse;
    private AssetDetailResponse assetDetail;
    private AssetResumeResponse assetResume;
    private UpdateConditionResponse conditionResponse;
    private AssignmentHistoryResponse historyEntry;

    @BeforeEach
    void setUp() {
        // Principal — mismo patrón resuelto en AssetAssignmentControllerTest
        AuthenticatedUser principal = new AuthenticatedUser(1L, "admin", "hashed", UserRole.ADMIN, true,1L);
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        principalPostProcessor = (MockHttpServletRequest request) -> {
            SecurityContext ctx = new SecurityContextImpl(authToken);
            SecurityContextHolder.setContext(ctx);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
            return request;
        };

        // ── AssetResponseDTO (POST response) ──
        assetResponse = new AssetResponseDTO();
        assetResponse.setId(1L);
        assetResponse.setInventoryNumber("INV-2024-00001");
        assetResponse.setDescription("Laptop Dell Inspiron");
        assetResponse.setCategoryName("Cómputo");
        assetResponse.setLocationName("Edificio A");
        assetResponse.setConditionStatus("GOOD");
        assetResponse.setLifecycleStatus("REGISTERED");
        assetResponse.setEntryDate(LocalDate.of(2024, 1, 15));

        // ── AssetDetailResponse (GET /{id}, /lookup, /barcode, /inventory-number) ──
        assetDetail = new AssetDetailResponse(
                1L, "INV-2024-00001", "BC-001", "Laptop Dell Inspiron",
                "Dell", "Inspiron 15", "SN-XYZ",
                "Cómputo", "Edificio A", "Edificio Principal", Campus.LOMA_BONITA,
                ConditionStatus.GOOD, LifecycleStatus.AVAILABLE,
                LocalDate.of(2024, 1, 15), LocalDateTime.of(2024, 6, 1, 10, 0),
                new GuardianSummary(2L, "Juan Pérez", "EMP-001", "TI"),
                List.of()
        );

        // ── AssetResumeResponse (GET / paginado) ──
        assetResume = new AssetResumeResponse(
                1L, "INV-2024-00001", "Laptop Dell Inspiron",
                "Dell", "Inspiron 15", "Cómputo", "Edificio A",
                ConditionStatus.GOOD, LifecycleStatus.AVAILABLE
        );

        // ── UpdateConditionResponse (PATCH /{id}/condition) ──
        conditionResponse = new UpdateConditionResponse(
                1L, "INV-2024-00001",
                ConditionStatus.GOOD, ConditionStatus.REGULAR,
                LocalDateTime.of(2024, 6, 10, 9, 0)
        );

        // ── AssignmentHistoryResponse (GET /{id}/assignments) ──
        historyEntry = new AssignmentHistoryResponse(
                10L, "Juan Pérez", "EMP-001", "Edificio A",
                LocalDateTime.of(2024, 3, 1, 8, 0), null,
                "admin", "Asignación inicial"
        );
    }

    // =========================================================================
    // POST /v1/assets
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/assets")
    class RegisterAsset {

        private AssetRequestDTO validRequest;

        @BeforeEach
        void setUpRequest() {
            validRequest = new AssetRequestDTO();
            validRequest.setDescription("Laptop Dell Inspiron");
            validRequest.setBrandId(1);
            validRequest.setCategoryId(2);
            validRequest.setLocationId(3);
            validRequest.setEntryDate(LocalDate.of(2024, 1, 15));
        }

        @Test
        @DisplayName("should_return201WithApiResponse_when_requestIsValid")
        void should_return201WithApiResponse_when_requestIsValid() throws Exception {
            when(assetService.registerAsset(any(AssetRequestDTO.class), eq(1L)))
                    .thenReturn(assetResponse);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.inventoryNumber").value("INV-2024-00001"))
                    .andExpect(jsonPath("$.data.description").value("Laptop Dell Inspiron"));

            verify(assetService).registerAsset(any(AssetRequestDTO.class), eq(1L));
        }

        @Test
        @DisplayName("should_return400_when_descriptionIsBlank")
        void should_return400_when_descriptionIsBlank() throws Exception {
            validRequest.setDescription("   ");

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return400_when_brandIdIsNull")
        void should_return400_when_brandIdIsNull() throws Exception {
            validRequest.setBrandId(null);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return400_when_categoryIdIsNull")
        void should_return400_when_categoryIdIsNull() throws Exception {
            validRequest.setCategoryId(null);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return400_when_entryDateIsNull")
        void should_return400_when_entryDateIsNull() throws Exception {
            validRequest.setEntryDate(null);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissing")
        void should_return400_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return409_when_barcodeAlreadyExists")
        void should_return409_when_barcodeAlreadyExists() throws Exception {
            when(assetService.registerAsset(any(), any()))
                    .thenThrow(new DuplicateResourceException("El código de barras ya está registrado"));

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("El código de barras ya está registrado"));
        }

        @Test
        @DisplayName("should_passAuthenticatedUserIdToService_when_principalIsPresent")
        void should_passAuthenticatedUserIdToService_when_principalIsPresent() throws Exception {
            when(assetService.registerAsset(any(), eq(1L))).thenReturn(assetResponse);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            verify(assetService).registerAsset(any(AssetRequestDTO.class), eq(1L));
        }
    }

    // =========================================================================
    // GET /v1/assets
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets")
    class GetAllAssets {

        @Test
        @DisplayName("should_return200WithPagedData_when_noFiltersApplied")
        void should_return200WithPagedData_when_noFiltersApplied() throws Exception {
            var page = new PageImpl<>(List.of(assetResume), PageRequest.of(0, 20), 1);

            // Usamos any() en todos los parámetros para garantizar que el mock siempre devuelva la página
            when(assetService.getAllAssets(any(), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // Volvemos a usar $.data porque el controlador sí lo está envolviendo
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(1L))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }



        @Test
        @DisplayName("should_return200WithFilteredData_when_conditionStatusIsProvided")
        void should_return200WithFilteredData_when_conditionStatusIsProvided() throws Exception {
            var page = new PageImpl<>(List.of(assetResume), PageRequest.of(0, 20), 1);
            when(assetService.getAllAssets(eq(ConditionStatus.GOOD), isNull(), isNull(), isNull(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL).param("conditionStatus", "GOOD"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].conditionStatus").value("GOOD"));

            verify(assetService).getAllAssets(eq(ConditionStatus.GOOD), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("should_return200WithFilteredData_when_dateRangeIsProvided")
        void should_return200WithFilteredData_when_dateRangeIsProvided() throws Exception {
            var page = new PageImpl<>(List.of(assetResume), PageRequest.of(0, 20), 1);
            when(assetService.getAllAssets(isNull(), isNull(),
                    eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 12, 31)), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL)
                            .param("startDate", "2024-01-01")
                            .param("endDate", "2024-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray());

            verify(assetService).getAllAssets(isNull(), isNull(),
                    eq(LocalDate.of(2024, 1, 1)), eq(LocalDate.of(2024, 12, 31)), any());
        }

        @Test
        @DisplayName("should_return200WithEmptyPage_when_noAssetsMatch")
        void should_return200WithEmptyPage_when_noAssetsMatch() throws Exception {
            var emptyPage = new PageImpl<AssetResumeResponse>(List.of(), PageRequest.of(0, 20), 0);
            when(assetService.getAllAssets(any(), any(), any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    // =========================================================================
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/{id}")
    class FindById {

        @Test
        @DisplayName("should_return200WithDetail_when_adminRequestsAnyAsset")
        void should_return200WithDetail_when_adminRequestsAnyAsset() throws Exception {
            // El principal del setUp() ya es ADMIN — lo reutilizamos directamente
            when(assetQueryService.findById(eq(1L), any(AuthenticatedUser.class)))
                    .thenReturn(assetDetail);

            mockMvc.perform(get(BASE_URL + "/1")
                            .with(principalPostProcessor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1L))
                    .andExpect(jsonPath("$.data.inventoryNumber").value("INV-2024-00001"))
                    .andExpect(jsonPath("$.data.conditionStatus").value("GOOD"))
                    .andExpect(jsonPath("$.data.guardian.fullName").value("Juan Pérez"));
        }

        @Test
        @DisplayName("should_return404_when_assetDoesNotExist")
        void should_return404_when_assetDoesNotExist() throws Exception {
            when(assetQueryService.findById(eq(99L), any(AuthenticatedUser.class)))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 99"));

            mockMvc.perform(get(BASE_URL + "/99")
                            .with(principalPostProcessor))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 99"));
        }

        @Test
        @DisplayName("should_return400_when_idIsNegative")
        void should_return400_when_idIsNegative() throws Exception {
            mockMvc.perform(get(BASE_URL + "/-1")
                            .with(principalPostProcessor))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetQueryService);
        }

        // ── Casos nuevos: acceso de GUARDIAN ─────────────────────────────────

        @Test
        @DisplayName("should_return200_when_guardianRequestsOwnAsset")
        void should_return200_when_guardianRequestsOwnAsset() throws Exception {
            // GUARDIAN con guardianId=2L solicita el bien 1L
            // que en assetDetail tiene GuardianSummary(id=2L) — es suyo
            AuthenticatedUser guardian = new AuthenticatedUser(
                    10L, "guardian01", "hashed", UserRole.GUARDIAN, true, 2L);

            UsernamePasswordAuthenticationToken guardianAuth =
                    new UsernamePasswordAuthenticationToken(guardian, null, guardian.getAuthorities());

            RequestPostProcessor guardianProcessor = request -> {
                SecurityContext ctx = new SecurityContextImpl(guardianAuth);
                SecurityContextHolder.setContext(ctx);
                request.getSession(true).setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
                return request;
            };

            when(assetQueryService.findById(eq(1L), any(AuthenticatedUser.class)))
                    .thenReturn(assetDetail);

            mockMvc.perform(get(BASE_URL + "/1")
                            .with(guardianProcessor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.guardian.fullName").value("Juan Pérez"));
        }

        @Test
        @DisplayName("should_return404_when_guardianRequestsAssetNotAssignedToThem")
        void should_return404_when_guardianRequestsAssetNotAssignedToThem() throws Exception {
            // GUARDIAN con guardianId=99L — el bien 1L pertenece a guardianId=2L
            AuthenticatedUser intruder = new AuthenticatedUser(
                    20L, "guardian02", "hashed", UserRole.GUARDIAN, true, 99L);

            UsernamePasswordAuthenticationToken intruderAuth =
                    new UsernamePasswordAuthenticationToken(intruder, null, intruder.getAuthorities());

            RequestPostProcessor intruderProcessor = request -> {
                SecurityContext ctx = new SecurityContextImpl(intruderAuth);
                SecurityContextHolder.setContext(ctx);
                request.getSession(true).setAttribute(
                        HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, ctx);
                return request;
            };

            // El servicio lanza 404 — la validación vive ahí, no en el controller
            when(assetQueryService.findById(eq(1L), any(AuthenticatedUser.class)))
                    .thenThrow(new ResourceNotFoundException("No se encontró el bien con ID: 1"));

            mockMvc.perform(get(BASE_URL + "/1")
                            .with(intruderProcessor))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =========================================================================
    // GET /v1/assets/lookup?q=
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/lookup")
    class Lookup {

        @Test
        @DisplayName("should_return200WithDetail_when_codeExists")
        void should_return200WithDetail_when_codeExists() throws Exception {
            when(assetQueryService.findByCode("INV-2024-00001")).thenReturn(assetDetail);

            mockMvc.perform(get(BASE_URL + "/lookup").param("q", "INV-2024-00001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.inventoryNumber").value("INV-2024-00001"));
        }

        @Test
        @DisplayName("should_return400_when_qParamIsBlank")
        void should_return400_when_qParamIsBlank() throws Exception {
            // @NotBlank sobre @RequestParam — falla con ConstraintViolationException (no MethodArgumentNotValidException)
            mockMvc.perform(get(BASE_URL + "/lookup").param("q", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetQueryService);
        }

        @Test
        @DisplayName("should_return400_when_qParamIsMissing")
        void should_return400_when_qParamIsMissing() throws Exception {
            mockMvc.perform(get(BASE_URL + "/lookup"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assetQueryService);
        }

        @Test
        @DisplayName("should_return400_when_qExceeds100Chars")
        void should_return400_when_qExceeds100Chars() throws Exception {
            String longCode = "A".repeat(101);

            mockMvc.perform(get(BASE_URL + "/lookup").param("q", longCode))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetQueryService);
        }

        @Test
        @DisplayName("should_return404_when_codeNotFound")
        void should_return404_when_codeNotFound() throws Exception {
            when(assetQueryService.findByCode("INVALID"))
                    .thenThrow(new ResourceNotFoundException("Código no encontrado: INVALID"));

            mockMvc.perform(get(BASE_URL + "/lookup").param("q", "INVALID"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =========================================================================
    // GET /v1/assets/search?keyword=
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/search")
    class SearchAssets {

        @Test
        @DisplayName("should_return200WithPage_when_keywordMatches")
        void should_return200WithPage_when_keywordMatches() throws Exception {
            AssetSearchResponseDTO dto = new AssetSearchResponseDTO(
                    1L, "INV-2024-00001", "Laptop Dell",
                    "Dell", "Inspiron", "Cómputo",
                    ConditionStatus.GOOD, LifecycleStatus.AVAILABLE,
                    "Edificio A", "Juan Pérez"
            );
            var page = new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1);
            when(assetService.searchAssets(eq("Dell"), any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL + "/search").param("keyword", "Dell"))
                    .andExpect(status().isOk())
                    // /search devuelve Page<> directamente, sin wrapper ApiResponse
                    .andExpect(jsonPath("$.content[0].inventoryNumber").value("INV-2024-00001"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should_return200WithAllAssets_when_keywordIsEmpty")
        void should_return200WithAllAssets_when_keywordIsEmpty() throws Exception {
            var page = new PageImpl<AssetSearchResponseDTO>(List.of(), PageRequest.of(0, 20), 0);
            when(assetService.searchAssets(eq(""), any())).thenReturn(page);

            // Edge case: keyword vacío usa defaultValue="" — el servicio decide qué devolver
            mockMvc.perform(get(BASE_URL + "/search"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray());

            verify(assetService).searchAssets(eq(""), any());
        }
    }

    // =========================================================================
    // GET /v1/assets/search/typeahead?q=&limit=
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/search/typeahead")
    class SearchTypeahead {

        @Test
        @DisplayName("should_return200WithPagedData_when_noFiltersApplied")
        void should_return200WithPagedData_when_noFiltersApplied() throws Exception {
            var page = new PageImpl<>(List.of(assetResume), PageRequest.of(0, 20), 1);

            // Usamos any() en todos los parámetros para garantizar que el mock siempre devuelva la página
            when(assetService.getAllAssets(any(), any(), any(), any(), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // Volvemos a usar $.data porque el controlador sí lo está envolviendo
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content[0].id").value(1L))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }


        @Test
        @DisplayName("should_return200WithEmptyList_when_queryHasLessThanTwoChars")
        void should_return200WithEmptyList_when_queryHasLessThanTwoChars() throws Exception {
            // Edge case: q de 1 char — el controller retorna lista vacía sin consultar el repositorio
            mockMvc.perform(get(BASE_URL + "/search/typeahead").param("q", "D"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());

            verifyNoInteractions(assetRepository);
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_queryIsBlank")
        void should_return200WithEmptyList_when_queryIsBlank() throws Exception {
            // Edge case: q vacío — también cortocircuita antes de consultar
            mockMvc.perform(get(BASE_URL + "/search/typeahead").param("q", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());

            verifyNoInteractions(assetRepository);
        }

        @Test
        @DisplayName("should_clampLimitTo30_when_limitExceedsMaximum")
        void should_clampLimitTo30_when_limitExceedsMaximum() throws Exception {
            // Edge case: limit=999 → el controller lo recorta a maxLimit=30
            when(assetRepository.searchActive(anyString(), eq(30))).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/search/typeahead")
                            .param("q", "Dell")
                            .param("limit", "999"))
                    .andExpect(status().isOk());

            verify(assetRepository).searchActive(anyString(), eq(30));
        }

        @Test
        @DisplayName("should_clampLimitTo1_when_limitIsZero")
        void should_clampLimitTo1_when_limitIsZero() throws Exception {
            // Edge case: limit=0 → el controller lo eleva a 1 (Math.max)
            when(assetRepository.searchActive(anyString(), eq(1))).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/search/typeahead")
                            .param("q", "Dell")
                            .param("limit", "0"))
                    .andExpect(status().isOk());

            verify(assetRepository).searchActive(anyString(), eq(1));
        }
    }

    // =========================================================================
    // GET /v1/assets/{id}/assignments
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/{id}/assignments")
    class GetAssignmentHistory {

        @Test
        @DisplayName("should_return200WithHistory_when_assetHasAssignments")
        void should_return200WithHistory_when_assetHasAssignments() throws Exception {
            when(assetQueryService.findAssignmentHistory(1L)).thenReturn(List.of(historyEntry));

            mockMvc.perform(get(BASE_URL + "/1/assignments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].id").value(10L))
                    .andExpect(jsonPath("$.data[0].guardianName").value("Juan Pérez"))
                    // returnedAt null → asignación activa
                    .andExpect(jsonPath("$.data[0].returnedAt").doesNotExist());
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_assetHasNoHistory")
        void should_return200WithEmptyList_when_assetHasNoHistory() throws Exception {
            when(assetQueryService.findAssignmentHistory(1L)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL + "/1/assignments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should_return404_when_assetDoesNotExist")
        void should_return404_when_assetDoesNotExist() throws Exception {
            when(assetQueryService.findAssignmentHistory(99L))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 99"));

            mockMvc.perform(get(BASE_URL + "/99/assignments"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_return400_when_idIsNegative")
        void should_return400_when_idIsNegative() throws Exception {
            mockMvc.perform(get(BASE_URL + "/-5/assignments"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assetQueryService);
        }
    }

    // =========================================================================
    // PATCH /v1/assets/{id}/condition
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/assets/{id}/condition")
    class UpdateCondition {

        private UpdateConditionRequest validRequest;

        @BeforeEach
        void setUpRequest() {
            validRequest = new UpdateConditionRequest(ConditionStatus.REGULAR);
        }

        @Test
        @DisplayName("should_return200WithUpdatedCondition_when_requestIsValid")
        void should_return200WithUpdatedCondition_when_requestIsValid() throws Exception {
            when(assetService.updateCondition(eq(1L), any(UpdateConditionRequest.class), anyLong()))
                    .thenReturn(conditionResponse);

            mockMvc.perform(patch(BASE_URL + "/1/condition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.assetId").value(1L))
                    .andExpect(jsonPath("$.data.previousCondition").value("GOOD"))
                    .andExpect(jsonPath("$.data.newCondition").value("REGULAR"));
        }

        @Test
        @DisplayName("should_return400_when_conditionStatusIsNull")
        void should_return400_when_conditionStatusIsNull() throws Exception {
            // Record con campo null — @NotNull dispara MethodArgumentNotValidException
            String bodyWithNullCondition = "{\"conditionStatus\": null}";

            mockMvc.perform(patch(BASE_URL + "/1/condition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithNullCondition))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return400_when_conditionStatusIsInvalidEnum")
        void should_return400_when_conditionStatusIsInvalidEnum() throws Exception {
            // Edge case: Jackson no puede deserializar un valor que no pertenece al enum
            String bodyWithInvalidEnum = "{\"conditionStatus\": \"EXCELENTE\"}";

            mockMvc.perform(patch(BASE_URL + "/1/condition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(bodyWithInvalidEnum))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return400_when_idIsNegative")
        void should_return400_when_idIsNegative() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/-1/condition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assetService);
        }

        @Test
        @DisplayName("should_return404_when_assetDoesNotExist")
        void should_return404_when_assetDoesNotExist() throws Exception {
            when(assetService.updateCondition(eq(99L), any(), anyLong()))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 99"));

            mockMvc.perform(patch(BASE_URL + "/99/condition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 99"));
        }
    }

    // =========================================================================
    // GET /v1/assets/next-folio
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/next-folio")
    class GetNextFolio {

        @Test
        @DisplayName("should_return200WithFolioFormat_when_sequenceExists")
        void should_return200WithFolioFormat_when_sequenceExists() throws Exception {
            when(assetRepository.getNextSequence(anyInt())).thenReturn(42L);

            mockMvc.perform(get(BASE_URL + "/next-folio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    // Verifica el formato INV-YYYY-#####
                    .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.matchesRegex("INV-\\d{4}-\\d{5}")));
        }

        @Test
        @DisplayName("should_return200WithFolio00042_when_sequenceIs42")
        void should_return200WithFolio00042_when_sequenceIs42() throws Exception {
            int currentYear = java.time.Year.now().getValue();
            when(assetRepository.getNextSequence(currentYear)).thenReturn(42L);

            mockMvc.perform(get(BASE_URL + "/next-folio"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value("INV-" + currentYear + "-00042"));
        }
    }

    // =========================================================================
    // Configuración interna del test — resuelve @AuthenticationPrincipal
    // =========================================================================

    @TestConfiguration
    static class TestWebConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}