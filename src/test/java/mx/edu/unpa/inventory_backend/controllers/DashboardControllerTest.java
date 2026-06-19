package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.dtos.dashboard.response.DashboardStatsResponse;
import mx.edu.unpa.inventory_backend.dtos.dashboard.response.LocationStatDTO;
import mx.edu.unpa.inventory_backend.enums.Campus;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {DashboardController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("DashboardController — /v1/dashboard/stats")
class DashboardControllerTest {

    private static final String ENDPOINT = "/v1/dashboard/stats";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ── Dependencias de seguridad — requeridas aunque Security esté excluida ──
    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Dependencia bajo prueba ───────────────────────────────────────────────
    @MockitoBean
    private DashboardService dashboardService;

    // =========================================================================
    // Factories — mantienen los tests legibles y el estado de fixtures central
    // =========================================================================

    /**
     * Construye un DashboardStatsResponse con datos representativos.
     * Cualquier variación en los tests debe partir de este método.
     */
    private DashboardStatsResponse buildFullResponse() {
        List<LocationStatDTO> topLocations = List.of(
                new LocationStatDTO("Edificio A", Campus.TUXTEPEC, 42L),
                new LocationStatDTO("Edificio B", Campus.LOMA_BONITA, 31L)
        );
        return new DashboardStatsResponse(
                150L,   // totalAssets
                80L,    // availableAssets
                50L,    // assignedAssets
                20L,    // inMaintenanceAssets
                100L,   // goodCondition
                30L,    // regularCondition
                20L,    // badCondition
                0L,     // openIncidents       (módulo no implementado)
                0L,     // maintenanceThisMonth (módulo no implementado)
                topLocations
        );
    }

    /** Respuesta válida con todos los contadores en cero — sistema recién instalado. */
    private DashboardStatsResponse buildZeroedResponse() {
        return new DashboardStatsResponse(
                0L, 0L, 0L, 0L,
                0L, 0L, 0L,
                0L, 0L,
                Collections.emptyList()
        );
    }

    // =========================================================================
    // Tests agrupados por escenario
    // =========================================================================

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @Test
        @DisplayName("should_return200WithFullStats_when_serviceReturnsData")
        void should_return200WithFullStats_when_serviceReturnsData() throws Exception {
            // Arrange
            DashboardStatsResponse response = buildFullResponse();
            when(dashboardService.getStats()).thenReturn(response);

            // Act & Assert — verificamos estructura completa del ApiResponse wrapper
            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isOk())

                    // Envelope ApiResponse
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").exists())

                    // KPIs
                    .andExpect(jsonPath("$.data.totalAssets").value(150))
                    .andExpect(jsonPath("$.data.availableAssets").value(80))
                    .andExpect(jsonPath("$.data.assignedAssets").value(50))
                    .andExpect(jsonPath("$.data.inMaintenanceAssets").value(20))

                    // Distribución por condición
                    .andExpect(jsonPath("$.data.goodCondition").value(100))
                    .andExpect(jsonPath("$.data.regularCondition").value(30))
                    .andExpect(jsonPath("$.data.badCondition").value(20))

                    // Módulos pendientes — siempre 0 hasta implementar
                    .andExpect(jsonPath("$.data.openIncidents").value(0))
                    .andExpect(jsonPath("$.data.maintenanceThisMonth").value(0))

                    // Top locations — estructura y datos
                    .andExpect(jsonPath("$.data.topLocations").isArray())
                    .andExpect(jsonPath("$.data.topLocations.length()").value(2))
                    .andExpect(jsonPath("$.data.topLocations[0].locationName").value("Edificio A"))
                    .andExpect(jsonPath("$.data.topLocations[0].campus").value("TUXTEPEC"))
                    .andExpect(jsonPath("$.data.topLocations[0].assetCount").value(42))
                    .andExpect(jsonPath("$.data.topLocations[1].locationName").value("Edificio B"))
                    .andExpect(jsonPath("$.data.topLocations[1].campus").value("LOMA_BONITA"));

            verify(dashboardService, times(1)).getStats();
        }

        @Test
        @DisplayName("should_returnSuccessTrueInWrapper_when_serviceReturnsData")
        void should_returnSuccessTrueInWrapper_when_serviceReturnsData() throws Exception {
            // El ApiResponse.ok() debe setear success=true — lo verificamos explícitamente.
            when(dashboardService.getStats()).thenReturn(buildFullResponse());

            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Edge cases — datos válidos pero límite")
    class EdgeCases {

        @Test
        @DisplayName("should_return200WithEmptyTopLocations_when_noLocationsExist")
        void should_return200WithEmptyTopLocations_when_noLocationsExist() throws Exception {
            // Edge case: El frontend recibe [] en topLocations, nunca null.
            // Un null aquí rompería el bar chart sin un mensaje de error claro.
            DashboardStatsResponse response = new DashboardStatsResponse(
                    10L, 10L, 0L, 0L,
                    10L, 0L, 0L,
                    0L, 0L,
                    Collections.emptyList() // <-- lista vacía, NO null
            );
            when(dashboardService.getStats()).thenReturn(response);

            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.topLocations").isArray())
                    .andExpect(jsonPath("$.data.topLocations").isEmpty());
        }

        @Test
        @DisplayName("should_return200WithAllZeroes_when_systemHasNoAssets")
        void should_return200WithAllZeroes_when_systemHasNoAssets() throws Exception {
            // Edge case: Sistema recién instalado — todos los KPIs en 0.
            // Verificamos que cero no sea confundido con "sin datos" por el serializador.
            when(dashboardService.getStats()).thenReturn(buildZeroedResponse());

            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalAssets").value(0))
                    .andExpect(jsonPath("$.data.availableAssets").value(0))
                    .andExpect(jsonPath("$.data.goodCondition").value(0))
                    .andExpect(jsonPath("$.data.topLocations").isEmpty());
        }

        @Test
        @DisplayName("should_callServiceExactlyOnce_when_requestReceived")
        void should_callServiceExactlyOnce_when_requestReceived() throws Exception {
            // Guardamos la capa de servicio: ningún retry silencioso ni llamada duplicada.
            when(dashboardService.getStats()).thenReturn(buildFullResponse());

            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isOk());

            verify(dashboardService, times(1)).getStats();
            verifyNoMoreInteractions(dashboardService);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Error handling — excepciones del servicio")
    class ErrorHandling {

        @Test
        @DisplayName("should_return500_when_serviceThrowsUnexpectedException")
        void should_return500_when_serviceThrowsUnexpectedException() throws Exception {
            // Edge case: el servicio falla (ej: BD caída, timeout).
            // El @RestControllerAdvice debe capturarlo y devolver 500.
            when(dashboardService.getStats())
                    .thenThrow(new RuntimeException("DB connection timeout"));

            mockMvc.perform(get(ENDPOINT))
                    .andExpect(status().isInternalServerError());
        }
    }
}