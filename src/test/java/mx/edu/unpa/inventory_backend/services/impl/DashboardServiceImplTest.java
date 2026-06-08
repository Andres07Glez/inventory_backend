package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.dtos.dashboard.response.DashboardStatsResponse;
import mx.edu.unpa.inventory_backend.dtos.dashboard.response.LocationStatDTO;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.IncidentRepository;
import mx.edu.unpa.inventory_backend.repositories.MaintenanceLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private AssetRepository          assetRepository;
    @Mock private IncidentRepository       incidentRepository;
    @Mock private MaintenanceLogRepository maintenanceLogRepository;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    // ── Helper ────────────────────────────────────────────────────────────────

    /**
     * Configura todos los mocks con valores por defecto (0 / lista vacía).
     * Cada test sobreescribe solo los valores que le interesan.
     * Evita stubbings innecesarios que Mockito marcaría como UnnecessaryStubbingException.
     */
    private void stubAllRepositoriesToZero() {
        when(assetRepository.countByLifecycleStatusNot(any())).thenReturn(0L);
        when(assetRepository.countByLifecycleStatus(any())).thenReturn(0L);
        when(assetRepository.countByConditionStatusAndLifecycleStatusNot(any(), any())).thenReturn(0L);
        when(incidentRepository.countByStatus(any())).thenReturn(0L);
        when(maintenanceLogRepository.countByPerformedDateBetween(any(), any())).thenReturn(0L);
        when(assetRepository.findTopLocationsByAssignedAssets(any())).thenReturn(List.of());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // KPIs de ciclo de vida
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnTotalAssetsExcludingDecommissioned_when_getStats() {
        // Arrange
        stubAllRepositoriesToZero();
        when(assetRepository.countByLifecycleStatusNot(LifecycleStatus.DECOMMISSIONED))
                .thenReturn(42L);

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.totalAssets()).isEqualTo(42L);
        verify(assetRepository).countByLifecycleStatusNot(LifecycleStatus.DECOMMISSIONED);
    }

    @Test
    void should_returnAvailableAssets_when_getStats() {
        // Arrange
        stubAllRepositoriesToZero();
        when(assetRepository.countByLifecycleStatus(LifecycleStatus.AVAILABLE)).thenReturn(15L);

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.availableAssets()).isEqualTo(15L);
    }

    @Test
    void should_returnAssignedAssets_when_getStats() {
        // Arrange
        stubAllRepositoriesToZero();
        when(assetRepository.countByLifecycleStatus(LifecycleStatus.ASSIGNED)).thenReturn(20L);

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.assignedAssets()).isEqualTo(20L);
    }

    @Test
    void should_returnInMaintenanceAssets_when_getStats() {
        // Arrange
        stubAllRepositoriesToZero();
        when(assetRepository.countByLifecycleStatus(LifecycleStatus.IN_MAINTENANCE)).thenReturn(3L);

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.inMaintenanceAssets()).isEqualTo(3L);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Distribución por condición (donut chart)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_excludeDecommissionedFromConditionCounts_when_getStats() {
        // Arrange — verificamos que los tres conteos excluyen DECOMMISSIONED
        stubAllRepositoriesToZero();
        when(assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.GOOD, LifecycleStatus.DECOMMISSIONED)).thenReturn(30L);
        when(assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.REGULAR, LifecycleStatus.DECOMMISSIONED)).thenReturn(10L);
        when(assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.BAD, LifecycleStatus.DECOMMISSIONED)).thenReturn(5L);

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.goodCondition()).isEqualTo(30L);
        assertThat(result.regularCondition()).isEqualTo(10L);
        assertThat(result.badCondition()).isEqualTo(5L);

        // Verificamos que NUNCA se llama sin el filtro de DECOMMISSIONED
        verify(assetRepository).countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.GOOD,    LifecycleStatus.DECOMMISSIONED);
        verify(assetRepository).countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.REGULAR, LifecycleStatus.DECOMMISSIONED);
        verify(assetRepository).countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.BAD,     LifecycleStatus.DECOMMISSIONED);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Módulos de gestión
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_countOpenIncidents_when_getStats() {
        // Arrange
        stubAllRepositoriesToZero();
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(7L);

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.openIncidents()).isEqualTo(7L);
        verify(incidentRepository).countByStatus(IncidentStatus.OPEN);
    }

    @Test
    void should_queryMaintenanceWithCurrentMonthBounds_when_getStats() {
        // Arrange — YearMonth.now() no es inyectable; usamos ArgumentCaptor para
        // verificar que las fechas pasadas son el primer y último día del mes actual.
        stubAllRepositoriesToZero();
        when(maintenanceLogRepository.countByPerformedDateBetween(any(), any())).thenReturn(4L);

        var startCaptor = ArgumentCaptor.forClass(LocalDate.class);
        var endCaptor   = ArgumentCaptor.forClass(LocalDate.class);

        YearMonth expectedMonth = YearMonth.now();

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert — valor correcto
        assertThat(result.maintenanceThisMonth()).isEqualTo(4L);

        // Assert — las fechas enviadas al repositorio corresponden al mes en curso
        verify(maintenanceLogRepository).countByPerformedDateBetween(
                startCaptor.capture(), endCaptor.capture());

        assertThat(startCaptor.getValue()).isEqualTo(expectedMonth.atDay(1));
        assertThat(endCaptor.getValue()).isEqualTo(expectedMonth.atEndOfMonth());
    }

    @Test
    void should_returnZeroMaintenance_when_noLogsThisMonth() {
        // Arrange
        stubAllRepositoriesToZero();
        // countByPerformedDateBetween ya stubbea a 0 en stubAllRepositoriesToZero()

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.maintenanceThisMonth()).isZero();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Top 5 ubicaciones
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_returnTopFiveLocations_when_locationsExist() {
        // Arrange
        stubAllRepositoriesToZero();
        var topLocations = List.of(
                new LocationStatDTO("Laboratorio A", "Campus Norte", 10L),
                new LocationStatDTO("Sala de Cómputo", "Campus Sur",  8L),
                new LocationStatDTO("Biblioteca",      "Campus Norte",  5L)
        );
        when(assetRepository.findTopLocationsByAssignedAssets(PageRequest.of(0, 5)))
                .thenReturn(topLocations);

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.topLocations()).hasSize(3);
        assertThat(result.topLocations().get(0).locationName()).isEqualTo("Laboratorio A");
        assertThat(result.topLocations().get(0).assetCount()).isEqualTo(10L);
    }

    @Test
    void should_requestExactlyFiveLocations_when_getStats() {
        // Arrange — verificamos que el PageRequest tiene size=5 y page=0
        stubAllRepositoriesToZero();
        var pageCaptor = ArgumentCaptor.forClass(PageRequest.class);
        when(assetRepository.findTopLocationsByAssignedAssets(any())).thenReturn(List.of());

        // Act
        dashboardService.getStats();

        // Assert
        verify(assetRepository).findTopLocationsByAssignedAssets(pageCaptor.capture());
        assertThat(pageCaptor.getValue().getPageSize()).isEqualTo(5);
        assertThat(pageCaptor.getValue().getPageNumber()).isZero();
    }

    @Test
    void should_returnEmptyLocationList_when_noAssignedAssets() {
        // Arrange
        stubAllRepositoriesToZero();
        when(assetRepository.findTopLocationsByAssignedAssets(any())).thenReturn(List.of());

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.topLocations()).isEmpty();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Respuesta completa (integración de todos los campos)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void should_assembleCompleteResponse_when_allRepositoriesReturnData() {
        // Arrange — happy path completo; verifica que ningún campo se mapea al lugar incorrecto
        when(assetRepository.countByLifecycleStatusNot(LifecycleStatus.DECOMMISSIONED)).thenReturn(100L);
        when(assetRepository.countByLifecycleStatus(LifecycleStatus.AVAILABLE)).thenReturn(30L);
        when(assetRepository.countByLifecycleStatus(LifecycleStatus.ASSIGNED)).thenReturn(50L);
        when(assetRepository.countByLifecycleStatus(LifecycleStatus.IN_MAINTENANCE)).thenReturn(5L);
        when(assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.GOOD,    LifecycleStatus.DECOMMISSIONED)).thenReturn(70L);
        when(assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.REGULAR, LifecycleStatus.DECOMMISSIONED)).thenReturn(20L);
        when(assetRepository.countByConditionStatusAndLifecycleStatusNot(
                ConditionStatus.BAD,     LifecycleStatus.DECOMMISSIONED)).thenReturn(10L);
        when(incidentRepository.countByStatus(IncidentStatus.OPEN)).thenReturn(3L);
        when(maintenanceLogRepository.countByPerformedDateBetween(any(), any())).thenReturn(8L);
        when(assetRepository.findTopLocationsByAssignedAssets(any())).thenReturn(
                List.of(new LocationStatDTO("Lab A", "Campus Norte", 15L))
        );

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert — cada campo apunta al valor correcto (sin transposiciones)
        assertThat(result.totalAssets()).isEqualTo(100L);
        assertThat(result.availableAssets()).isEqualTo(30L);
        assertThat(result.assignedAssets()).isEqualTo(50L);
        assertThat(result.inMaintenanceAssets()).isEqualTo(5L);
        assertThat(result.goodCondition()).isEqualTo(70L);
        assertThat(result.regularCondition()).isEqualTo(20L);
        assertThat(result.badCondition()).isEqualTo(10L);
        assertThat(result.openIncidents()).isEqualTo(3L);
        assertThat(result.maintenanceThisMonth()).isEqualTo(8L);
        assertThat(result.topLocations()).hasSize(1);
    }

    @Test
    void should_returnAllZerosAndEmptyList_when_systemHasNoAssets() {
        // Arrange — sistema vacío (primer arranque)
        stubAllRepositoriesToZero();

        // Act
        DashboardStatsResponse result = dashboardService.getStats();

        // Assert
        assertThat(result.totalAssets()).isZero();
        assertThat(result.availableAssets()).isZero();
        assertThat(result.assignedAssets()).isZero();
        assertThat(result.inMaintenanceAssets()).isZero();
        assertThat(result.goodCondition()).isZero();
        assertThat(result.regularCondition()).isZero();
        assertThat(result.badCondition()).isZero();
        assertThat(result.openIncidents()).isZero();
        assertThat(result.maintenanceThisMonth()).isZero();
        assertThat(result.topLocations()).isEmpty();
    }
}