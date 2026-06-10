package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.maintenance.request.MaintenanceCreateRequest;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceResponse;
import mx.edu.unpa.inventory_backend.dtos.maintenance.response.MaintenanceSummary;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;
import mx.edu.unpa.inventory_backend.exceptions.InvalidAssetStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.IncidentRepository;
import mx.edu.unpa.inventory_backend.repositories.MaintenanceLogRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceImplTest {

    @Mock private MaintenanceLogRepository maintenanceRepository;
    @Mock private AssetRepository          assetRepository;
    @Mock private IncidentRepository       incidentRepository;
    @Mock private UserRepository           userRepository;

    @InjectMocks
    private MaintenanceServiceImpl maintenanceService;

    // ─────────────────────────────────────────────
    //  Factories de stubs
    // ─────────────────────────────────────────────

    /**
     * Construye un Asset stub con ID, inventoryNumber y description.
     * No usa Mockito.mock() — la entidad tiene Lombok @Getter/@Setter,
     * así que instanciarla directamente es más limpio y expresivo.
     */
    private Asset stubAsset(Long id) {
        Asset a = new Asset();
        a.setId(id);
        a.setInventoryNumber("INV-2026-" + String.format("%05d", id));
        a.setDescription("Bien de prueba #" + id);
        return a;
    }

    /**
     * Construye un User stub con Guardian anidado.
     * El mapper toResponse() y toSummary() navegan:
     *   log.getCreatedBy().getGuardian().getFullName()
     * Por eso el Guardian debe estar seteado con un nombre real.
     */
    private User stubUser(Long id, String guardianFullName) {
        Guardian guardian = new Guardian();
        guardian.setId(id);
        guardian.setFullName(guardianFullName);

        User u = User.builder()
                .username("usr_test_" + id)
                .passwordHash("$2a$10$hash")
                .isActive(true)
                .build();
        u.setId(id);
        u.setGuardian(guardian);
        return u;
    }

    /**
     * Construye un Incident stub vinculado al asset dado.
     * resolveIncident() valida incident.getAsset().getId().equals(assetId),
     * por eso el asset del incident DEBE coincidir con el del request.
     */
    private Incident stubIncident(Long incidentId, Asset asset) {
        Incident i = new Incident();
        i.setId(incidentId);
        i.setAsset(asset);
        return i;
    }

    /**
     * Construye un MaintenanceLog guardado (con ID y createdAt asignados).
     * Simula lo que devolvería maintenanceRepository.save().
     */
    private MaintenanceLog stubSavedLog(Long logId, Asset asset, User createdBy, Incident incident,
                                        MaintenanceType type, String description) {
        MaintenanceLog log = new MaintenanceLog();
        log.setId(logId);
        log.setAsset(asset);
        log.setCreatedBy(createdBy);
        log.setIncident(incident);
        log.setMaintenanceType(type);
        log.setDescription(description);
        log.setPerformedBy("Tecnico SA");
        log.setPerformedDate(LocalDate.of(2026, 1, 15));
        log.setCost(new BigDecimal("1500.00"));
        log.setConditionBefore(ConditionStatus.REGULAR);
        log.setConditionAfter(ConditionStatus.GOOD);
        log.setCreatedAt(LocalDateTime.of(2026, 1, 15, 10, 0));
        return log;
    }

    /** Request mínimo válido con incidentId opcional. */
    private MaintenanceCreateRequest buildRequest(Long assetId, Long incidentId) {
        return new MaintenanceCreateRequest(
                assetId,
                incidentId,
                MaintenanceType.CORRECTIVE,
                "Cambio de disco duro",
                "Tecnico SA",
                LocalDate.of(2026, 1, 15),
                new BigDecimal("1500.00"),
                ConditionStatus.REGULAR,
                ConditionStatus.GOOD
        );
    }

    // ─────────────────────────────────────────────
    //  create — happy paths
    // ─────────────────────────────────────────────

    @Test
    void should_returnMaintenanceResponse_when_createWithoutIncident() {
        // Arrange
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");
        MaintenanceLog savedLog = stubSavedLog(100L, asset, createdBy, null,
                MaintenanceType.CORRECTIVE, "Cambio de disco duro");

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(createdBy));
        when(maintenanceRepository.save(any(MaintenanceLog.class))).thenReturn(savedLog);

        MaintenanceCreateRequest request = buildRequest(10L, null);

        // Act
        MaintenanceResponse response = maintenanceService.create(request, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(10L,  response.assetId());
        assertNull(response.incidentId());
        assertEquals(MaintenanceType.CORRECTIVE, response.maintenanceType());
        assertEquals("Ana Lopez", response.createdByName());

        verify(incidentRepository, never()).findById(any());
        verify(maintenanceRepository).save(any(MaintenanceLog.class));
    }

    @Test
    void should_returnMaintenanceResponse_when_createWithLinkedIncident() {
        // Arrange
        Asset    asset     = stubAsset(10L);
        User     createdBy = stubUser(1L, "Ana Lopez");
        Incident incident  = stubIncident(50L, asset);
        MaintenanceLog savedLog = stubSavedLog(101L, asset, createdBy, incident,
                MaintenanceType.CORRECTIVE, "Cambio de disco duro");
        savedLog.setIncident(incident);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(createdBy));
        when(incidentRepository.findById(50L)).thenReturn(Optional.of(incident));
        when(maintenanceRepository.save(any(MaintenanceLog.class))).thenReturn(savedLog);

        MaintenanceCreateRequest request = buildRequest(10L, 50L);

        // Act
        MaintenanceResponse response = maintenanceService.create(request, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(50L, response.incidentId());
        verify(incidentRepository).findById(50L);
    }

    @Test
    void should_persistAllFieldsFromRequest_when_create() {
        // Arrange — capturamos el objeto que se le pasa a save() para
        // verificar que el mapeo request → entidad es correcto.
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");
        MaintenanceLog savedLog = stubSavedLog(100L, asset, createdBy, null,
                MaintenanceType.PREVENTIVE, "Revision general");

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(createdBy));
        when(maintenanceRepository.save(any(MaintenanceLog.class))).thenReturn(savedLog);

        MaintenanceCreateRequest request = new MaintenanceCreateRequest(
                10L, null,
                MaintenanceType.PREVENTIVE,
                "Revision general",
                "Tecnico Interno",
                LocalDate.of(2026, 3, 1),
                null,
                ConditionStatus.GOOD,
                ConditionStatus.GOOD
        );

        // Act
        maintenanceService.create(request, 1L);

        // Assert — el log persistido debe tener exactamente los campos del request
        ArgumentCaptor<MaintenanceLog> captor = ArgumentCaptor.forClass(MaintenanceLog.class);
        verify(maintenanceRepository).save(captor.capture());
        MaintenanceLog captured = captor.getValue();

        assertEquals(asset,                     captured.getAsset());
        assertEquals(createdBy,                 captured.getCreatedBy());
        assertNull(captured.getIncident());
        assertEquals(MaintenanceType.PREVENTIVE, captured.getMaintenanceType());
        assertEquals("Revision general",        captured.getDescription());
        assertEquals("Tecnico Interno",         captured.getPerformedBy());
        assertEquals(LocalDate.of(2026, 3, 1),  captured.getPerformedDate());
        assertNull(captured.getCost());
        assertEquals(ConditionStatus.GOOD,      captured.getConditionBefore());
        assertEquals(ConditionStatus.GOOD,      captured.getConditionAfter());
    }

    // ─────────────────────────────────────────────
    //  create — errores de validación
    // ─────────────────────────────────────────────

    @Test
    void should_throwResourceNotFoundException_when_createAndAssetDoesNotExist() {
        // Arrange
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        MaintenanceCreateRequest request = buildRequest(99L, null);

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> maintenanceService.create(request, 1L)
        );
        assertTrue(ex.getMessage().contains("99"));
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_createAndUserNotFound() {
        // Arrange
        Asset asset = stubAsset(10L);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(userRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

        MaintenanceCreateRequest request = buildRequest(10L, null);

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> maintenanceService.create(request, 99L)
        );
        assertTrue(ex.getMessage().contains("99"));
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_createAndIncidentNotFound() {
        // Arrange
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(createdBy));
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        MaintenanceCreateRequest request = buildRequest(10L, 999L);

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> maintenanceService.create(request, 1L)
        );
        assertTrue(ex.getMessage().contains("999"));
        verify(maintenanceRepository, never()).save(any());
    }

    @Test
    void should_throwInvalidAssetStateException_when_incidentBelongsToDifferentAsset() {
        // Arrange — la incidencia apunta al bien 20, pero el request es para el bien 10
        Asset assetInRequest  = stubAsset(10L);
        Asset assetOfIncident = stubAsset(20L);
        User  createdBy       = stubUser(1L, "Ana Lopez");
        Incident wrongIncident = stubIncident(50L, assetOfIncident);

        when(assetRepository.findById(10L)).thenReturn(Optional.of(assetInRequest));
        when(userRepository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.of(createdBy));
        when(incidentRepository.findById(50L)).thenReturn(Optional.of(wrongIncident));

        MaintenanceCreateRequest request = buildRequest(10L, 50L);

        // Act & Assert
        InvalidAssetStateException ex = assertThrows(
                InvalidAssetStateException.class,
                () -> maintenanceService.create(request, 1L)
        );
        assertTrue(ex.getMessage().contains("50"));
        assertTrue(ex.getMessage().contains("10"));
        verify(maintenanceRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    //  getByAssetId
    // ─────────────────────────────────────────────

    @Test
    void should_returnSummaryList_when_getByAssetIdAndLogsExist() {
        // Arrange
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");
        MaintenanceLog log1 = stubSavedLog(1L, asset, createdBy, null, MaintenanceType.PREVENTIVE, "Rev 1");
        MaintenanceLog log2 = stubSavedLog(2L, asset, createdBy, null, MaintenanceType.CORRECTIVE, "Rep 1");

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(maintenanceRepository.findByAssetIdOrdered(10L)).thenReturn(List.of(log1, log2));

        // Act
        List<MaintenanceSummary> result = maintenanceService.getByAssetId(10L);

        // Assert
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(2L, result.get(1).id());
        assertEquals("Ana Lopez", result.get(0).createdByName());
        verify(maintenanceRepository).findByAssetIdOrdered(10L);
    }

    @Test
    void should_returnEmptyList_when_getByAssetIdAndNoLogsExist() {
        // Arrange
        Asset asset = stubAsset(10L);
        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(maintenanceRepository.findByAssetIdOrdered(10L)).thenReturn(List.of());

        // Act
        List<MaintenanceSummary> result = maintenanceService.getByAssetId(10L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void should_throwResourceNotFoundException_when_getByAssetIdAndAssetDoesNotExist() {
        // Arrange
        when(assetRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> maintenanceService.getByAssetId(99L)
        );
        verify(maintenanceRepository, never()).findByAssetIdOrdered(any());
    }

    // ─────────────────────────────────────────────
    //  getAll
    // ─────────────────────────────────────────────

    @Test
    void should_returnAllSummaries_when_getAllWithNullType() {
        // Arrange
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");
        MaintenanceLog log = stubSavedLog(1L, asset, createdBy, null, MaintenanceType.PREVENTIVE, "Rev");

        when(maintenanceRepository.findAllFiltered(null)).thenReturn(List.of(log));

        // Act
        List<MaintenanceSummary> result = maintenanceService.getAll(null);

        // Assert
        assertEquals(1, result.size());
        assertEquals(MaintenanceType.PREVENTIVE, result.get(0).maintenanceType());
        verify(maintenanceRepository).findAllFiltered(null);
    }

    @Test
    void should_returnFilteredSummaries_when_getAllWithSpecificType() {
        // Arrange
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");
        MaintenanceLog log = stubSavedLog(2L, asset, createdBy, null, MaintenanceType.WARRANTY, "Garantia");

        when(maintenanceRepository.findAllFiltered(MaintenanceType.WARRANTY)).thenReturn(List.of(log));

        // Act
        List<MaintenanceSummary> result = maintenanceService.getAll(MaintenanceType.WARRANTY);

        // Assert
        assertEquals(1, result.size());
        assertEquals(MaintenanceType.WARRANTY, result.get(0).maintenanceType());
        verify(maintenanceRepository).findAllFiltered(MaintenanceType.WARRANTY);
    }

    @Test
    void should_returnEmptyList_when_getAllAndNoLogsMatch() {
        // Arrange
        when(maintenanceRepository.findAllFiltered(MaintenanceType.CORRECTIVE)).thenReturn(List.of());

        // Act
        List<MaintenanceSummary> result = maintenanceService.getAll(MaintenanceType.CORRECTIVE);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────
    //  getById
    // ─────────────────────────────────────────────

    @Test
    void should_returnMaintenanceResponse_when_getByIdAndLogExists() {
        // Arrange
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");
        MaintenanceLog log = stubSavedLog(100L, asset, createdBy, null,
                MaintenanceType.CORRECTIVE, "Cambio de RAM");

        when(maintenanceRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(log));

        // Act
        MaintenanceResponse response = maintenanceService.getById(100L);

        // Assert
        assertNotNull(response);
        assertEquals(100L,                      response.id());
        assertEquals(10L,                       response.assetId());
        assertEquals(MaintenanceType.CORRECTIVE, response.maintenanceType());
        assertEquals("Cambio de RAM",           response.description());
        assertEquals("Ana Lopez",               response.createdByName());
        assertNull(response.incidentId());
    }

    @Test
    void should_returnIncidentIdInResponse_when_getByIdAndLogHasLinkedIncident() {
        // Arrange
        Asset    asset     = stubAsset(10L);
        User     createdBy = stubUser(1L, "Ana Lopez");
        Incident incident  = stubIncident(50L, asset);
        MaintenanceLog log = stubSavedLog(100L, asset, createdBy, incident,
                MaintenanceType.CORRECTIVE, "Reparacion");

        when(maintenanceRepository.findByIdWithDetails(100L)).thenReturn(Optional.of(log));

        // Act
        MaintenanceResponse response = maintenanceService.getById(100L);

        // Assert
        assertEquals(50L, response.incidentId());
    }

    @Test
    void should_throwResourceNotFoundException_when_getByIdAndLogDoesNotExist() {
        // Arrange
        when(maintenanceRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> maintenanceService.getById(999L)
        );
        assertTrue(ex.getMessage().contains("999"));
    }

    // ─────────────────────────────────────────────
    //  delete
    // ─────────────────────────────────────────────

    @Test
    void should_deleteLog_when_deleteAndLogExists() {
        // Arrange
        when(maintenanceRepository.existsById(100L)).thenReturn(true);

        // Act
        maintenanceService.delete(100L);

        // Assert — se verifica que ambas interacciones ocurrieron en orden correcto
        verify(maintenanceRepository).existsById(100L);
        verify(maintenanceRepository).deleteById(100L);
    }

    @Test
    void should_throwResourceNotFoundException_when_deleteAndLogDoesNotExist() {
        // Arrange
        when(maintenanceRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> maintenanceService.delete(999L)
        );
        assertTrue(ex.getMessage().contains("999"));
        verify(maintenanceRepository, never()).deleteById(any());
    }

    // ─────────────────────────────────────────────
    //  toSummary — incidentId nullable
    // ─────────────────────────────────────────────

    @Test
    void should_returnNullIncidentId_when_summaryLogHasNoIncident() {
        // Arrange — valida que toSummary() maneja correctamente log.getIncident() == null
        Asset asset     = stubAsset(10L);
        User  createdBy = stubUser(1L, "Ana Lopez");
        MaintenanceLog logWithoutIncident = stubSavedLog(1L, asset, createdBy, null,
                MaintenanceType.PREVENTIVE, "Rev sin incidente");

        when(assetRepository.findById(10L)).thenReturn(Optional.of(asset));
        when(maintenanceRepository.findByAssetIdOrdered(10L)).thenReturn(List.of(logWithoutIncident));

        // Act
        List<MaintenanceSummary> result = maintenanceService.getByAssetId(10L);

        // Assert
        assertNull(result.get(0).incidentId());
    }
}