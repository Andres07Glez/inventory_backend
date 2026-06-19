package mx.edu.unpa.inventory_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentCloseRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.request.IncidentStatusUpdateDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.RepairType;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.InvalidIncidentStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.IncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {IncidentController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(IncidentControllerTest.CustomTestConfig.class)
class IncidentControllerTest {

    // ── Configuración de prueba ───────────────────────────────────────────────

    @org.springframework.boot.test.context.TestConfiguration
    static class CustomTestConfig implements WebMvcConfigurer {

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(
                        org.springframework.core.MethodParameter parameter) {
                    return parameter.getParameterType()
                            .equals(AuthenticatedUser.class);
                }

                @Override
                public Object resolveArgument(
                        org.springframework.core.MethodParameter parameter,
                        org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                        org.springframework.web.context.request.NativeWebRequest webRequest,
                        org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                    return new AuthenticatedUser(
                            1L, "user@unpa.mx", "pwd_hash", UserRole.OPERADOR, true);
                }
            });
        }
    }

    // ── Dependencias ─────────────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IncidentService incidentService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private IncidentResponseDTO sampleResponse;
    private IncidentSummaryDTO sampleSummary;

    @BeforeEach
    void setUp() {
        sampleResponse = new IncidentResponseDTO(
                1L,
                "INC-2026-00001",
                10L,
                "INV-001",
                "Laptop Dell",
                "Pantalla rota",
                RepairType.INTERNAL,
                IncidentStatus.OPEN,
                ConditionStatus.BAD,
                LocalDate.of(2026, 1, 15),
                null,
                null,
                null,
                LocalDateTime.of(2026, 1, 15, 10, 0),
                "user@unpa.mx",
                List.of()
        );

        sampleSummary = new IncidentSummaryDTO(
                1L,
                "INC-2026-00001",
                "Pantalla rota",
                IncidentStatus.OPEN,
                ConditionStatus.BAD,
                RepairType.INTERNAL,
                LocalDateTime.of(2026, 1, 15, 10, 0),
                LocalDate.of(2026, 1, 15),
                "user@unpa.mx"
        );
    }

    // ── GET /v1/incidents ─────────────────────────────────────────────────────

    @Test
    void should_returnPagedIncidents_when_listCalledWithNoFilters() throws Exception {
        Page<IncidentSummaryDTO> page = new PageImpl<>(List.of(sampleSummary));
        when(incidentService.list(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].folio").value("INC-2026-00001"))
                .andExpect(jsonPath("$.data.content[0].status").value("OPEN"));
    }

    @Test
    void should_returnPagedIncidents_when_listCalledWithStatusFilter() throws Exception {
        Page<IncidentSummaryDTO> page = new PageImpl<>(List.of(sampleSummary));
        when(incidentService.list(eq(IncidentStatus.OPEN), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/v1/incidents").param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].status").value("OPEN"));
    }

    @Test
    void should_returnPagedIncidents_when_listCalledWithAssetIdAndFolioFilter() throws Exception {
        Page<IncidentSummaryDTO> page = new PageImpl<>(List.of(sampleSummary));
        when(incidentService.list(isNull(), eq(10L), eq("INC-2026-00001"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/v1/incidents")
                        .param("assetId", "10")
                        .param("folio", "INC-2026-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // ── GET /v1/assets/{assetId}/incidents ────────────────────────────────────

    @Test
    void should_returnIncidentList_when_listByAssetCalledWithValidAssetId() throws Exception {
        when(incidentService.listByAsset(10L)).thenReturn(List.of(sampleSummary));

        mockMvc.perform(get("/v1/assets/10/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].folio").value("INC-2026-00001"))
                .andExpect(jsonPath("$.data[0].description").value("Pantalla rota"));
    }

    @Test
    void should_return404_when_listByAssetCalledWithNonExistentAssetId() throws Exception {
        doThrow(new ResourceNotFoundException("Bien no encontrado con id: 99"))
                .when(incidentService).listByAsset(99L);

        mockMvc.perform(get("/v1/assets/99/incidents"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 99"));
    }

    @Test
    void should_return400_when_listByAssetCalledWithNegativeAssetId() throws Exception {
        mockMvc.perform(get("/v1/assets/-1/incidents"))
                .andExpect(status().isBadRequest());
    }

    // ── GET /v1/incidents/{id} ────────────────────────────────────────────────

    @Test
    void should_returnIncidentDetail_when_getByIdCalledWithValidId() throws Exception {
        when(incidentService.getById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/v1/incidents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.folio").value("INC-2026-00001"))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.assetId").value(10))
                .andExpect(jsonPath("$.data.conditionAtIncident").value("BAD"));
    }

    @Test
    void should_return404_when_getByIdCalledWithNonExistentId() throws Exception {
        doThrow(new ResourceNotFoundException("Incidencia no encontrada con id: 99"))
                .when(incidentService).getById(99L);

        mockMvc.perform(get("/v1/incidents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 99"));
    }

    @Test
    void should_return400_when_getByIdCalledWithNegativeId() throws Exception {
        mockMvc.perform(get("/v1/incidents/-1"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /v1/incidents ────────────────────────────────────────────────────

    @Test
    void should_return201AndCreatedIncident_when_createCalledWithValidRequest() throws Exception {
        IncidentRequestDTO request = new IncidentRequestDTO(
                10L,
                "Pantalla rota",
                LocalDate.of(2026, 1, 15),
                ConditionStatus.BAD,
                RepairType.INTERNAL
        );

        when(incidentService.create(any(IncidentRequestDTO.class), eq(1L)))
                .thenReturn(sampleResponse);

        mockMvc.perform(post("/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.folio").value("INC-2026-00001"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    void should_return400_when_createCalledWithNullAssetId() throws Exception {
        IncidentRequestDTO request = new IncidentRequestDTO(
                null,              // assetId nulo → @NotNull falla
                "Pantalla rota",
                LocalDate.of(2026, 1, 15),
                ConditionStatus.BAD,
                RepairType.INTERNAL
        );

        mockMvc.perform(post("/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_createCalledWithBlankDescription() throws Exception {
        IncidentRequestDTO request = new IncidentRequestDTO(
                10L,
                "",               // descripción en blanco → @NotBlank falla
                LocalDate.of(2026, 1, 15),
                ConditionStatus.BAD,
                RepairType.INTERNAL
        );

        mockMvc.perform(post("/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_createCalledWithNullConditionAtIncident() throws Exception {
        IncidentRequestDTO request = new IncidentRequestDTO(
                10L,
                "Pantalla rota",
                LocalDate.of(2026, 1, 15),
                null,             // conditionAtIncident nulo → @NotNull falla
                RepairType.INTERNAL
        );

        mockMvc.perform(post("/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_createCalledWithFutureIncidentDate() throws Exception {
        IncidentRequestDTO request = new IncidentRequestDTO(
                10L,
                "Pantalla rota",
                LocalDate.now().plusDays(5),   // fecha futura → @PastOrPresent falla
                ConditionStatus.BAD,
                RepairType.INTERNAL
        );

        mockMvc.perform(post("/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return404_when_createCalledWithNonExistentAsset() throws Exception {
        IncidentRequestDTO request = new IncidentRequestDTO(
                999L,
                "Pantalla rota",
                LocalDate.of(2026, 1, 15),
                ConditionStatus.BAD,
                RepairType.INTERNAL
        );

        doThrow(new ResourceNotFoundException("Bien no encontrado con id: 999"))
                .when(incidentService).create(any(IncidentRequestDTO.class), anyLong());

        mockMvc.perform(post("/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 999"));
    }

    @Test
    void should_return400_when_createCalledWithEmptyBody() throws Exception {
        mockMvc.perform(post("/v1/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ── PATCH /v1/incidents/{id}/status ───────────────────────────────────────

    @Test
    void should_returnUpdatedIncident_when_updateStatusCalledWithValidTransition() throws Exception {
        IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);

        IncidentResponseDTO inProgress = new IncidentResponseDTO(
                1L, "INC-2026-00001", 10L, "INV-001", "Laptop Dell",
                "Pantalla rota", RepairType.INTERNAL, IncidentStatus.IN_PROGRESS,
                ConditionStatus.BAD, LocalDate.of(2026, 1, 15),
                null, null, null,
                LocalDateTime.of(2026, 1, 15, 10, 0), "user@unpa.mx", List.of()
        );

        when(incidentService.updateStatus(eq(1L), any(IncidentStatusUpdateDTO.class)))
                .thenReturn(inProgress);

        mockMvc.perform(patch("/v1/incidents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    void should_return409_when_updateStatusCalledWithInvalidTransition() throws Exception {
        IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.CLOSED);

        doThrow(new InvalidIncidentStateException(
                "Transición de estado inválida: OPEN → CLOSED"))
                .when(incidentService).updateStatus(eq(1L), any(IncidentStatusUpdateDTO.class));

        mockMvc.perform(patch("/v1/incidents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Transición de estado inválida: OPEN → CLOSED"));
    }

    @Test
    void should_return400_when_updateStatusCalledWithNullStatus() throws Exception {
        IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(null);

        mockMvc.perform(patch("/v1/incidents/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return404_when_updateStatusCalledWithNonExistentIncident() throws Exception {
        IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);

        doThrow(new ResourceNotFoundException("Incidencia no encontrada con id: 99"))
                .when(incidentService).updateStatus(eq(99L), any(IncidentStatusUpdateDTO.class));

        mockMvc.perform(patch("/v1/incidents/99/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 99"));
    }

    @Test
    void should_return400_when_updateStatusCalledWithNegativeId() throws Exception {
        IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);

        mockMvc.perform(patch("/v1/incidents/-1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    // ── POST /v1/incidents/{id}/close ─────────────────────────────────────────

    @Test
    void should_returnClosedIncident_when_closeCalledWithValidRequest() throws Exception {
        IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO(
                "Pantalla reemplazada correctamente",
                RepairType.INTERNAL
        );

        IncidentResponseDTO closed = new IncidentResponseDTO(
                1L, "INC-2026-00001", 10L, "INV-001", "Laptop Dell",
                "Pantalla rota", RepairType.INTERNAL, IncidentStatus.CLOSED,
                ConditionStatus.BAD, LocalDate.of(2026, 1, 15),
                "Pantalla reemplazada correctamente",
                LocalDateTime.of(2026, 6, 11, 14, 0), "user@unpa.mx",
                LocalDateTime.of(2026, 1, 15, 10, 0), "user@unpa.mx", List.of()
        );

        when(incidentService.close(eq(1L), any(IncidentCloseRequestDTO.class), eq(1L)))
                .thenReturn(closed);

        mockMvc.perform(post("/v1/incidents/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.resolutionNotes")
                        .value("Pantalla reemplazada correctamente"));
    }

    @Test
    void should_return400_when_closeCalledWithBlankResolutionNotes() throws Exception {
        IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO(
                "",               // notas en blanco → @NotBlank falla
                RepairType.INTERNAL
        );

        mockMvc.perform(post("/v1/incidents/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return409_when_closeCalledOnIncidentNotInResolvedState() throws Exception {
        IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO(
                "Intentando cerrar incidencia abierta",
                RepairType.EXTERNAL
        );

        doThrow(new InvalidIncidentStateException(
                "Solo se pueden cerrar incidencias en estado RESOLVED"))
                .when(incidentService).close(eq(1L), any(IncidentCloseRequestDTO.class), eq(1L));

        mockMvc.perform(post("/v1/incidents/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Solo se pueden cerrar incidencias en estado RESOLVED"));
    }

    @Test
    void should_return404_when_closeCalledWithNonExistentIncident() throws Exception {
        IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO(
                "Notas de resolución",
                RepairType.INTERNAL
        );

        doThrow(new ResourceNotFoundException("Incidencia no encontrada con id: 99"))
                .when(incidentService).close(eq(99L), any(IncidentCloseRequestDTO.class), eq(1L));

        mockMvc.perform(post("/v1/incidents/99/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 99"));
    }

    @Test
    void should_return400_when_closeCalledWithNegativeId() throws Exception {
        IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO(
                "Notas de resolución",
                RepairType.INTERNAL
        );

        mockMvc.perform(post("/v1/incidents/-1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_closeCalledWithMissingBody() throws Exception {
        mockMvc.perform(post("/v1/incidents/1/close")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}