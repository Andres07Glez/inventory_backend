package mx.edu.unpa.inventory_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mx.edu.unpa.inventory_backend.dtos.decommission.request.DecommissionRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.InvalidDecommissionStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.DecommissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice test para DecommissionController.
 *
 * Diseño de seguridad en tests:
 *   addFilters=false desactiva la cadena de filtros (JWT no corre).
 *   AuthenticationPrincipalArgumentResolver sigue activo porque NO se excluye
 *   SecurityAutoConfiguration — esa exclusión rompería el resolver.
 *   El principal se inyecta por test vía .with(authentication(...)).
 */
@WebMvcTest(controllers = DecommissionController.class,excludeAutoConfiguration = SecurityAutoConfiguration.class)
@Import({GlobalExceptionHandler.class,DecommissionControllerTest.CustomTestConfig.class})
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DecommissionController — /v1/decommissions")
class DecommissionControllerTest {

    private static final String BASE_URL     = "/v1/decommissions";
    private static final String BY_ID_URL    = "/v1/decommissions/{id}";
    private static final String CONFIRM_URL  = "/v1/decommissions/{id}/confirm";
    private static final String BY_ASSET_URL = "/v1/assets/{assetId}/decommission";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockitoBean private DecommissionService decommissionService;

    // =========================================================================
    // Factories
    // =========================================================================

    private DecommissionResponseDTO buildResponseDTO(Long id, DecommissionStatus status) {
        return new DecommissionResponseDTO(
                id,
                10L, "INV-001", "Laptop Dell",
                null, null,
                "Daño irreparable",
                null,
                LocalDate.of(2024, 6, 1),
                status,
                LocalDateTime.of(2024, 6, 1, 10, 0), "Juan Pérez",
                null, null
        );
    }

    private DecommissionSummaryDTO buildSummaryDTO(Long id) {
        return new DecommissionSummaryDTO(
                id,
                10L, "INV-001", "Laptop Dell",
                DecommissionStatus.PENDING,
                LocalDate.of(2024, 6, 1),
                LocalDateTime.of(2024, 6, 1, 10, 0),
                "Juan Pérez",
                false
        );
    }

    private MockMultipartFile buildRequestPart(Long assetId, Long incidentId, String justification)
            throws Exception {
        DecommissionRequestDTO dto = new DecommissionRequestDTO(
                assetId, incidentId, justification, LocalDate.of(2024, 6, 1));
        return new MockMultipartFile(
                "request",
                "request.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(dto)
        );
    }

    private MockMultipartFile buildPdfDocument() {
        return new MockMultipartFile(
                "document", "baja.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "PDF-CONTENT".getBytes()
        );
    }

    // =========================================================================
    // GET /v1/decommissions — Listado paginado
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/decommissions — listado paginado")
    class ListEndpoint {

        @Test
        @DisplayName("should_return200WithPage_when_noStatusFilterProvided")
        void should_return200WithPage_when_noStatusFilterProvided() throws Exception {
            var page = new PageImpl<>(
                    List.of(buildSummaryDTO(1L)), PageRequest.of(0, 20), 1);
            when(decommissionService.list(isNull(), any())).thenReturn(page);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                    .andExpect(jsonPath("$.data.content[0].hasLinkedIncident").value(false))
                    .andExpect(jsonPath("$.data.totalElements").value(1));

            verify(decommissionService).list(isNull(), any());
        }

        @Test
        @DisplayName("should_return200FilteredByStatus_when_statusParamProvided")
        void should_return200FilteredByStatus_when_statusParamProvided() throws Exception {
            var page = new PageImpl<>(
                    List.of(buildSummaryDTO(2L)), PageRequest.of(0, 20), 1);
            when(decommissionService.list(eq(DecommissionStatus.CONFIRMED), any()))
                    .thenReturn(page);

            mockMvc.perform(get(BASE_URL).param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(2));

            verify(decommissionService).list(eq(DecommissionStatus.CONFIRMED), any());
        }

        @Test
        @DisplayName("should_return200WithEmptyPage_when_noDecommissionsExist")
        void should_return200WithEmptyPage_when_noDecommissionsExist() throws Exception {
            var emptyPage = new PageImpl<DecommissionSummaryDTO>(
                    List.of(), PageRequest.of(0, 20), 0);
            when(decommissionService.list(any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test
        @DisplayName("should_return400_when_statusParamIsInvalidEnum") // Cambiamos el nombre
        void should_return400_when_statusParamIsInvalidEnum() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .param("status", "VALOR_INVALIDO")
                            .with(user("testuser").authorities(new SimpleGrantedAuthority("ROLE_OPERADOR"))))
                    .andExpect(status().isBadRequest());
        }
    }

    // =========================================================================
    // GET /v1/decommissions/{id} — Detalle por ID
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/decommissions/{id} — detalle por ID")
    class GetById {

        @Test
        @DisplayName("should_return200WithDecommission_when_idExists")
        void should_return200WithDecommission_when_idExists() throws Exception {
            when(decommissionService.getById(1L))
                    .thenReturn(buildResponseDTO(1L, DecommissionStatus.PENDING));

            mockMvc.perform(get(BY_ID_URL, 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.assetInventoryNumber").value("INV-001"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should_return404_when_idDoesNotExist")
        void should_return404_when_idDoesNotExist() throws Exception {
            when(decommissionService.getById(99L))
                    .thenThrow(new ResourceNotFoundException("Baja no encontrada con id: 99"));

            mockMvc.perform(get(BY_ID_URL, 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_return400_when_idIsZero")
        void should_return400_when_idIsZero() throws Exception {
            mockMvc.perform(get(BY_ID_URL, 0L))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }

        @Test
        @DisplayName("should_return400_when_idIsNegative")
        void should_return400_when_idIsNegative() throws Exception {
            mockMvc.perform(get(BY_ID_URL, -1L))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }
    }

    // =========================================================================
    // GET /v1/assets/{assetId}/decommission — Detalle por bien
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/{assetId}/decommission — detalle por bien")
    class GetByAsset {

        @Test
        @DisplayName("should_return200WithDecommission_when_assetHasDecommission")
        void should_return200WithDecommission_when_assetHasDecommission() throws Exception {
            when(decommissionService.getByAssetId(10L))
                    .thenReturn(buildResponseDTO(1L, DecommissionStatus.CONFIRMED));

            mockMvc.perform(get(BY_ASSET_URL, 10L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.assetId").value(10))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should_return404_when_assetHasNoDecommission")
        void should_return404_when_assetHasNoDecommission() throws Exception {
            when(decommissionService.getByAssetId(99L))
                    .thenThrow(new ResourceNotFoundException("El bien 99 no tiene baja registrada"));

            mockMvc.perform(get(BY_ASSET_URL, 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_return400_when_assetIdIsNegative")
        void should_return400_when_assetIdIsNegative() throws Exception {
            mockMvc.perform(get(BY_ASSET_URL, -5L))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }
    }

    // =========================================================================
    // POST /v1/decommissions — Crear baja (multipart/form-data)
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/decommissions — crear baja")
    class Create {

        private UsernamePasswordAuthenticationToken buildAuthentication(Long userId) {
            var principal = new AuthenticatedUser(userId, "user@unpa.mx", "pwd_hash", UserRole.OPERADOR, true,1L);
            return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        }

        @Test
        @DisplayName("should_return201_when_validRequestWithoutDocument")
        void should_return201_when_validRequestWithoutDocument() throws Exception {
            // document es OPCIONAL — el frontend puede no adjuntar PDF.
            MockMultipartFile requestPart = buildRequestPart(10L, null, "Daño irreparable");
            when(decommissionService.create(any(), isNull(), anyLong()))
                    .thenReturn(buildResponseDTO(1L, DecommissionStatus.PENDING));

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart)
                            .with(authentication(buildAuthentication(1L))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("should_return201_when_validRequestWithDocument")
        void should_return201_when_validRequestWithDocument() throws Exception {
            MockMultipartFile requestPart = buildRequestPart(10L, null, "Daño irreparable");
            MockMultipartFile document    = buildPdfDocument();
            when(decommissionService.create(any(), any(), anyLong()))
                    .thenReturn(buildResponseDTO(1L, DecommissionStatus.PENDING));

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart)
                            .file(document)
                            .with(authentication(buildAuthentication(1L))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(1));

            verify(decommissionService).create(
                    any(),
                    argThat(f -> f != null && !f.isEmpty()),
                    anyLong()
            );
        }

        @Test
        @DisplayName("should_return201_when_validRequestWithLinkedIncident")
        void should_return201_when_validRequestWithLinkedIncident() throws Exception {
            // incidentId es opcional; cuando se provee, debe llegar al servicio sin perderse.
            MockMultipartFile requestPart = buildRequestPart(10L, 55L, "Bien dañado en incidente");
            when(decommissionService.create(
                    argThat(r -> r.incidentId() != null && r.incidentId().equals(55L)),
                    any(), anyLong()))
                    .thenReturn(buildResponseDTO(1L, DecommissionStatus.PENDING));

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart)
                            .with(authentication(buildAuthentication(1L))))
                    .andExpect(status().isCreated());

            verify(decommissionService).create(
                    argThat(r -> Long.valueOf(55L).equals(r.incidentId())),
                    any(), anyLong()
            );
        }

        @Test
        @DisplayName("should_return400_when_assetIdIsNull")
        void should_return400_when_assetIdIsNull() throws Exception {
            MockMultipartFile requestPart = buildRequestPart(null, null, "Justificación válida");

            mockMvc.perform(multipart(BASE_URL).file(requestPart))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }

        @Test
        @DisplayName("should_return400_when_justificationIsBlank")
        void should_return400_when_justificationIsBlank() throws Exception {
            MockMultipartFile requestPart = buildRequestPart(10L, null, "   ");

            mockMvc.perform(multipart(BASE_URL).file(requestPart))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }

        @Test
        @DisplayName("should_return400_when_justificationIsEmpty")
        void should_return400_when_justificationIsEmpty() throws Exception {
            MockMultipartFile requestPart = buildRequestPart(10L, null, "");

            mockMvc.perform(multipart(BASE_URL).file(requestPart))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }

        @Test
        @DisplayName("should_return400_when_assetIdIsNegative")
        void should_return400_when_assetIdIsNegative() throws Exception {
            MockMultipartFile requestPart = buildRequestPart(-1L, null, "Justificación válida");

            mockMvc.perform(multipart(BASE_URL).file(requestPart))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }

        @Test
        @DisplayName("should_return400_when_requestPartIsMissing")
        void should_return400_when_requestPartIsMissing() throws Exception {
            mockMvc.perform(multipart(BASE_URL).file(buildPdfDocument()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }

        @Test
        @DisplayName("should_return404_when_assetDoesNotExist")
        void should_return404_when_assetDoesNotExist() throws Exception {
            MockMultipartFile requestPart = buildRequestPart(999L, null, "Justificación válida");
            when(decommissionService.create(any(), any(), anyLong()))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 999"));

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart)
                            .with(authentication(buildAuthentication(1L))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_return409_when_assetAlreadyHasDecommission")
        void should_return409_when_assetAlreadyHasDecommission() throws Exception {
            MockMultipartFile requestPart = buildRequestPart(10L, null, "Justificación válida");
            when(decommissionService.create(any(), any(), anyLong()))
                    .thenThrow(new InvalidDecommissionStateException("El bien ya tiene un proceso de baja activo"));

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart)
                            .with(authentication(buildAuthentication(1L))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =========================================================================
    // PATCH /v1/decommissions/{id}/confirm — Confirmar baja
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/decommissions/{id}/confirm — confirmar baja")
    class Confirm {

        private UsernamePasswordAuthenticationToken buildAuthentication() {
            var principal = new AuthenticatedUser(1L, "admin@unpa.mx", "pwd_hash", UserRole.ADMIN, true,1L);
            return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        }

        @Test
        @DisplayName("should_return200WithConfirmedStatus_when_adminConfirms")
        void should_return200WithConfirmedStatus_when_adminConfirms() throws Exception {
            when(decommissionService.confirm(eq(1L), anyLong()))
                    .thenReturn(buildResponseDTO(1L, DecommissionStatus.CONFIRMED));

            mockMvc.perform(patch(CONFIRM_URL, 1L)
                            .with(authentication(buildAuthentication())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should_return404_when_decommissionIdDoesNotExist")
        void should_return404_when_decommissionIdDoesNotExist() throws Exception {
            when(decommissionService.confirm(eq(99L), anyLong()))
                    .thenThrow(new ResourceNotFoundException("Baja no encontrada con id: 99"));

            mockMvc.perform(patch(CONFIRM_URL, 99L)
                            .with(authentication(buildAuthentication())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_return409_when_decommissionAlreadyConfirmed")
        void should_return409_when_decommissionAlreadyConfirmed() throws Exception {
            when(decommissionService.confirm(eq(1L), anyLong()))
                    .thenThrow(new InvalidDecommissionStateException("La baja ya fue confirmada"));

            mockMvc.perform(patch(CONFIRM_URL, 1L)
                            .with(authentication(buildAuthentication())))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("should_return400_when_idIsNegative")
        void should_return400_when_idIsNegative() throws Exception {
            mockMvc.perform(patch(CONFIRM_URL, -1L)
                            .with(authentication(buildAuthentication())))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(decommissionService);
        }


    }
    @TestConfiguration
    static class CustomTestConfig implements WebMvcConfigurer {

        // 1. La configuración de Jackson para las fechas (que ya tenías)
        @Bean
        @Primary
        ObjectMapper objectMapper() {
            return JsonMapper.builder()
                    .addModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .build();
        }


        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {

                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    // Solo aplica si el controlador pide un AuthenticatedUser
                    return parameter.getParameterType().equals(AuthenticatedUser.class);
                }

                @Override
                public Object resolveArgument(MethodParameter parameter,
                                              ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest,
                                              WebDataBinderFactory binderFactory) {
                    // Inyectamos un usuario quemado con ID 1L
                    // (1L es el que esperan todos tus verify() en los mocks)
                    return new AuthenticatedUser(
                            1L,
                            "user@unpa.mx",
                            "pwd_hash",
                            UserRole.OPERADOR,
                            true,
                            1L
                    );
                }
            });
        }
    }

}