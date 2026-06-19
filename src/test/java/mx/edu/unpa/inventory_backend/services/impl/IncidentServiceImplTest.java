package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.incident.request.*;
import mx.edu.unpa.inventory_backend.dtos.incident.response.*;
import mx.edu.unpa.inventory_backend.enums.*;
import mx.edu.unpa.inventory_backend.exceptions.InvalidIncidentStateException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.*;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceImplTest {

    // ── Dependencias mockeadas ────────────────────────────────────────────────

    @Mock private IncidentRepository         incidentRepository;
    @Mock private AssetRepository            assetRepository;
    @Mock private UserRepository             userRepository;
    @Mock private StorageService             storageService;
    @Mock private AssetAssignmentRepository  assetAssignmentRepository;

    @InjectMocks
    private IncidentServiceImpl service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private User     operator;
    private Asset    asset;
    private Incident incident;

    @BeforeEach
    void setUp() {
        Guardian guardian = new Guardian();
        guardian.setId(1L);
        guardian.setFullName("Ana García");

        operator = User.builder()
                .id(1L)
                .username("operador01")
                .passwordHash("$2a$hash")
                .role(UserRole.OPERADOR)
                .isActive(true)
                .guardian(guardian)
                .build();

        Category category = new Category();
        category.setId(1);
        category.setName("Equipo de Cómputo");

        asset = new Asset();
        asset.setId(5L);
        asset.setInventoryNumber("INV-2026-00005");
        asset.setDescription("Laptop Dell Latitude 5520");
        asset.setCategory(category);
        asset.setEntryDate(LocalDate.of(2024, 1, 1));
        asset.setLifecycleStatus(LifecycleStatus.AVAILABLE);
        asset.setConditionStatus(ConditionStatus.GOOD);
        asset.setCreatedBy(operator);
        asset.setUpdatedBy(operator);

        // Fixture base: incidencia OPEN sin imágenes.
        // images = new ArrayList<>() por defecto en la entidad →
        // storageService.buildPublicUrl NUNCA se invoca → no se stubea en ningún test.
        incident = new Incident();
        incident.setId(1L);
        incident.setAsset(asset);
        incident.setDescription("Pantalla dañada por golpe");
        incident.setIncidentDate(LocalDate.of(2026, 6, 1));
        incident.setConditionAtIncident(ConditionStatus.GOOD);
        incident.setRepairType(RepairType.EXTERNAL);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedBy(operator);
        incident.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Nested
    class Create {

        @Test
        void should_returnOpenIncident_when_validRequest() {
            // Arrange
            IncidentRequestDTO request = new IncidentRequestDTO(
                    asset.getId(), "Pantalla rota", LocalDate.of(2026, 6, 1),
                    ConditionStatus.BAD, RepairType.EXTERNAL);

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            // save() recibe un objeto Incident NUEVO (construido en el servicio, no el fixture)
            when(incidentRepository.save(any(Incident.class))).thenReturn(incident);

            // Act
            IncidentResponseDTO result = service.create(request, operator.getId());

            // Assert
            assertThat(result.status()).isEqualTo(IncidentStatus.OPEN);
            assertThat(result.assetId()).isEqualTo(asset.getId());
            assertThat(result.folio()).matches("INC-\\d{4}-00001");
            verify(incidentRepository).save(any(Incident.class));
        }

        @Test
        void should_useLocalDateNow_when_incidentDateIsNull() {
            // Arrange
            IncidentRequestDTO request = new IncidentRequestDTO(
                    asset.getId(), "Descripción", null, ConditionStatus.GOOD, null);

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
                Incident saved = inv.getArgument(0);
                assertThat(saved.getIncidentDate()).isEqualTo(LocalDate.now());
                return incident;
            });

            // Act & Assert
            assertThatNoException().isThrownBy(() -> service.create(request, operator.getId()));
        }

        @Test
        void should_setConditionAtIncidentFromAsset_when_creating() {
            // El campo conditionAtIncident se toma del bien, NO del request
            asset.setConditionStatus(ConditionStatus.REGULAR);
            IncidentRequestDTO request = new IncidentRequestDTO(
                    asset.getId(), "Descripción", LocalDate.of(2026, 5, 1),
                    ConditionStatus.BAD, null);

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> {
                Incident saved = inv.getArgument(0);
                assertThat(saved.getConditionAtIncident()).isEqualTo(ConditionStatus.REGULAR);
                return incident;
            });

            service.create(request, operator.getId());
        }

        @Test
        void should_acceptDate_when_incidentDateIsExactlyJanuary1st2002() {
            // Boundary: MIN_INCIDENT_DATE = 2002-01-01. isBefore(MIN) es false → debe pasar.
            IncidentRequestDTO request = new IncidentRequestDTO(
                    asset.getId(), "Descripción", LocalDate.of(2002, 1, 1),
                    ConditionStatus.GOOD, null);

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(incidentRepository.save(any())).thenReturn(incident);

            assertThatNoException().isThrownBy(() -> service.create(request, operator.getId()));
        }

        @Test
        void should_throwInvalidIncidentStateException_when_incidentDateBefore2002() {
            // Boundary: 2001-12-31 es estrictamente antes del mínimo permitido
            IncidentRequestDTO request = new IncidentRequestDTO(
                    asset.getId(), "Descripción", LocalDate.of(2001, 12, 31),
                    ConditionStatus.GOOD, null);
            Long operatorId = operator.getId();
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));

            InvalidIncidentStateException ex = assertThrows(
                    InvalidIncidentStateException.class,
                    () -> service.create(request, operatorId));
            assertTrue(ex.getMessage().contains("2002"));
        }

        @Test
        void should_throwInvalidIncidentStateException_when_assetIsDecommissioned() {
            asset.setLifecycleStatus(LifecycleStatus.DECOMMISSIONED);
            IncidentRequestDTO request = new IncidentRequestDTO(
                    asset.getId(), "Descripción", LocalDate.of(2026, 6, 1),
                    ConditionStatus.BAD, null);
            Long operatorId = operator.getId();
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));

            InvalidIncidentStateException ex = assertThrows(
                    InvalidIncidentStateException.class,
                    () -> service.create(request, operatorId));
            assertTrue(ex.getMessage().contains("dado de baja"));

            // El usuario nunca se consulta si el bien ya está dado de baja
            verify(userRepository, never()).findByIdAndIsActiveTrue(anyLong());
        }

        @Test
        void should_throwResourceNotFoundException_when_assetNotFound() {
            IncidentRequestDTO request = new IncidentRequestDTO(
                    999L, "Descripción", LocalDate.of(2026, 6, 1), ConditionStatus.GOOD, null);
            Long operatorId =operator.getId();
            when(assetRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.create(request, operatorId));
            assertTrue(ex.getMessage().contains("Bien no encontrado"));
            assertTrue(ex.getMessage().contains("999"));
        }

        @Test
        void should_throwResourceNotFoundException_when_createdByUserNotFound() {
            IncidentRequestDTO request = new IncidentRequestDTO(
                    asset.getId(), "Descripción", LocalDate.of(2026, 6, 1),
                    ConditionStatus.GOOD, null);

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(userRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.create(request, 99L));
            assertTrue(ex.getMessage().contains("Usuario no encontrado"));
        }
    }

    // =========================================================================
    // getById()
    // =========================================================================

    @Nested
    class GetById {

        @Test
        void should_returnIncidentDTO_when_idExists() {
            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));

            IncidentResponseDTO result = service.getById(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.folio()).matches("INC-\\d{4}-00001");
            assertThat(result.assetInventoryNumber()).isEqualTo(asset.getInventoryNumber());
            assertThat(result.createdByName()).isEqualTo(operator.getGuardian().getFullName());
        }

        @Test
        void should_throwResourceNotFoundException_when_idNotFound() {
            when(incidentRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.getById(999L));
            assertTrue(ex.getMessage().contains("Incidencia no encontrada"));
            assertTrue(ex.getMessage().contains("999"));
        }
    }

    // =========================================================================
    // list() y listByAsset()
    // =========================================================================

    @Nested
    class Queries {

        @Test
        void should_returnPagedSummaries_when_filterByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Incident> page = new PageImpl<>(List.of(incident), pageable, 1);
            when(incidentRepository.findAllFiltered(IncidentStatus.OPEN, null, null, pageable))
                    .thenReturn(page);

            Page<IncidentSummaryDTO> result =
                    service.list(IncidentStatus.OPEN, null, null, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(IncidentStatus.OPEN);
        }

        @Test
        void should_extractNumericIdAndPassToRepo_when_folioQueryIsValid() {
            // "INC-2026-00042" → extractIdFromFolio → 42L → findAllFiltered recibe 42L
            Pageable pageable = PageRequest.of(0, 10);
            Page<Incident> page = new PageImpl<>(List.of(incident), pageable, 1);
            when(incidentRepository.findAllFiltered(null, null, 42L, pageable))
                    .thenReturn(page);

            service.list(null, null, "INC-2026-00042", pageable);

            verify(incidentRepository).findAllFiltered(null, null, 42L, pageable);
        }

        @Test
        void should_passNullIdToRepo_when_folioQueryHasInvalidFormat() {
            // Un folio malformado no debe romper el listado — solo ignora el filtro
            Pageable pageable = PageRequest.of(0, 10);
            when(incidentRepository.findAllFiltered(null, null, null, pageable))
                    .thenReturn(Page.empty(pageable));

            service.list(null, null, "FOLIO_INVALIDO", pageable);

            verify(incidentRepository).findAllFiltered(null, null, null, pageable);
        }

        @Test
        void should_returnIncidentList_when_assetHasIncidents() {
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(incidentRepository.findAllByAssetId(asset.getId()))
                    .thenReturn(List.of(incident));

            List<IncidentSummaryDTO> result = service.listByAsset(asset.getId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(incident.getId());
            assertThat(result.get(0).folio()).matches("INC-\\d{4}-00001");
        }

        @Test
        void should_returnEmptyList_when_assetHasNoIncidents() {
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(incidentRepository.findAllByAssetId(asset.getId())).thenReturn(List.of());

            assertThat(service.listByAsset(asset.getId())).isEmpty();
        }

        @Test
        void should_throwResourceNotFoundException_when_assetNotFoundInListByAsset() {
            when(assetRepository.findById(999L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.listByAsset(999L));
            assertTrue(ex.getMessage().contains("Bien no encontrado"));

            verify(incidentRepository, never()).findAllByAssetId(anyLong());
        }
    }

    // =========================================================================
    // updateStatus()
    // =========================================================================

    @Nested
    class UpdateStatus {

        @Test
        void should_markAssetInMaintenance_when_transitionToInProgressWithNoActiveAssignment() {
            // Edge case: bien sin asignación activa — solo se actualiza el lifecycle
            incident.setStatus(IncidentStatus.OPEN);
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));
            when(assetAssignmentRepository.findActiveByAssetId(asset.getId()))
                    .thenReturn(Optional.empty());
            when(incidentRepository.save(any())).thenReturn(incident);

            service.updateStatus(1L, dto);

            assertThat(asset.getLifecycleStatus()).isEqualTo(LifecycleStatus.IN_MAINTENANCE);
            verify(assetRepository).save(asset);
            verify(assetAssignmentRepository, never()).save(any());
        }

        @Test
        void should_closeActiveAssignmentAndMarkInMaintenance_when_transitionToInProgressWithAssignment() {
            incident.setStatus(IncidentStatus.OPEN);
            AssetAssignment assignment = buildActiveAssignment();
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));
            when(assetAssignmentRepository.findActiveByAssetId(asset.getId()))
                    .thenReturn(Optional.of(assignment));
            when(incidentRepository.save(any())).thenReturn(incident);

            service.updateStatus(1L, dto);

            // La asignación activa debe quedar cerrada
            assertThat(assignment.getReturnedAt()).isNotNull();
            verify(assetAssignmentRepository).save(assignment);
            // El bien debe pasar a IN_MAINTENANCE
            assertThat(asset.getLifecycleStatus()).isEqualTo(LifecycleStatus.IN_MAINTENANCE);
            verify(assetRepository).save(asset);
        }

        @Test
        void should_beIdempotent_when_assetAlreadyInMaintenanceOnInProgressTransition() {
            // Edge case: bien ya en IN_MAINTENANCE — no debe lanzar excepción
            asset.setLifecycleStatus(LifecycleStatus.IN_MAINTENANCE);
            incident.setStatus(IncidentStatus.OPEN);
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));
            when(assetAssignmentRepository.findActiveByAssetId(asset.getId()))
                    .thenReturn(Optional.empty());
            when(incidentRepository.save(any())).thenReturn(incident);

            assertThatNoException().isThrownBy(() -> service.updateStatus(1L, dto));
            assertThat(asset.getLifecycleStatus()).isEqualTo(LifecycleStatus.IN_MAINTENANCE);
        }

        @Test
        void should_transitionToResolved_when_incidentIsInProgress() {
            incident.setStatus(IncidentStatus.IN_PROGRESS);
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.RESOLVED);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));
            when(incidentRepository.save(any())).thenReturn(incident);

            service.updateStatus(1L, dto);

            assertThat(incident.getStatus()).isEqualTo(IncidentStatus.RESOLVED);
            // returnAssetToInventory NO se llama para esta transición
            verifyNoInteractions(assetAssignmentRepository);
            verify(assetRepository, never()).save(any());
        }

        @Test
        void should_throwInvalidIncidentStateException_when_transitionOpenToResolved() {
            // Saltar IN_PROGRESS no está permitido
            incident.setStatus(IncidentStatus.OPEN);
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.RESOLVED);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));

            InvalidIncidentStateException ex = assertThrows(
                    InvalidIncidentStateException.class,
                    () -> service.updateStatus(1L, dto));
            assertTrue(ex.getMessage().contains("OPEN"));
            assertTrue(ex.getMessage().contains("RESOLVED"));
        }

        @Test
        void should_throwInvalidIncidentStateException_when_transitionInProgressToOpen() {
            // Retroceder en el flujo está prohibido
            incident.setStatus(IncidentStatus.IN_PROGRESS);
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.OPEN);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));

            InvalidIncidentStateException ex = assertThrows(
                    InvalidIncidentStateException.class,
                    () -> service.updateStatus(1L, dto));
            assertTrue(ex.getMessage().contains("IN_PROGRESS"));
            assertTrue(ex.getMessage().contains("OPEN"));
        }

        @Test
        void should_throwInvalidIncidentStateException_when_closingResolvedViaStatusEndpoint() {
            // RESOLVED → CLOSED se debe gestionar por /close, no por /status
            incident.setStatus(IncidentStatus.RESOLVED);
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.CLOSED);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));

            InvalidIncidentStateException ex = assertThrows(
                    InvalidIncidentStateException.class,
                    () -> service.updateStatus(1L, dto));
            assertTrue(ex.getMessage().contains("/close"));
        }

        @Test
        void should_throwInvalidIncidentStateException_when_incidentIsAlreadyClosed() {
            incident.setStatus(IncidentStatus.CLOSED);
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));

            assertThatThrownBy(() -> service.updateStatus(1L, dto))
                    .isInstanceOf(InvalidIncidentStateException.class)
                    .hasMessageContaining("CLOSED");
        }

        @Test
        void should_throwResourceNotFoundException_when_incidentNotFound() {
            when(incidentRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());
            IncidentStatusUpdateDTO dto = new IncidentStatusUpdateDTO(IncidentStatus.IN_PROGRESS);
            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.updateStatus(999L,
                            dto));
            assertTrue(ex.getMessage().contains("Incidencia no encontrada"));
            assertTrue(ex.getMessage().contains("999"));
        }
    }

    // =========================================================================
    // close()
    // =========================================================================

    @Nested
    class Close {

        @Test
        void should_closeIncidentAndSetAuditFields_when_statusIsResolved() {
            incident.setStatus(IncidentStatus.RESOLVED);
            IncidentCloseRequestDTO dto =
                    new IncidentCloseRequestDTO("Pantalla reemplazada", RepairType.EXTERNAL);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(incidentRepository.save(any())).thenReturn(incident);

            IncidentResponseDTO result = service.close(1L, dto, operator.getId());

            // Efectos de negocio sobre el objeto de dominio
            assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLOSED);
            assertThat(incident.getResolutionNotes()).isEqualTo("Pantalla reemplazada");
            assertThat(incident.getResolvedAt()).isNotNull();
            assertThat(incident.getResolvedBy()).isEqualTo(operator);
            // El DTO de respuesta debe reflejar los cambios
            assertThat(result).isNotNull();
        }

        @Test
        void should_throwInvalidIncidentStateException_when_statusIsOpen() {
            incident.setStatus(IncidentStatus.OPEN);
            IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO("Notas", null);
            Long operatorId = operator.getId();
            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));

            InvalidIncidentStateException ex = assertThrows(
                    InvalidIncidentStateException.class,
                    () -> service.close(1L, dto, operatorId));
            assertTrue(ex.getMessage().contains("RESOLVED"));
            assertTrue(ex.getMessage().contains("Estado actual: OPEN"));

            verify(userRepository, never()).findByIdAndIsActiveTrue(anyLong());
        }

        @Test
        void should_throwInvalidIncidentStateException_when_statusIsInProgress() {
            incident.setStatus(IncidentStatus.IN_PROGRESS);
            IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO("Notas", null);
            Long operatorId = operator.getId();
            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));

            InvalidIncidentStateException ex = assertThrows(
                    InvalidIncidentStateException.class,
                    () -> service.close(1L, dto, operatorId));
            assertTrue(ex.getMessage().contains("Estado actual: IN_PROGRESS"));
        }

        @Test
        void should_throwResourceNotFoundException_when_incidentNotFound() {
            when(incidentRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());
            Long operatorId = operator.getId();
            IncidentCloseRequestDTO request = new IncidentCloseRequestDTO("Notas", null); // <-- Extraído aquí

            // Act & Assert: La lambda ahora tiene exactamente UNA SOLA invocación
            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.close(999L, request, operatorId)
            );

            assertTrue(ex.getMessage().contains("Incidencia no encontrada"));
        }

        @Test
        void should_throwResourceNotFoundException_when_closedByUserNotFound() {
            incident.setStatus(IncidentStatus.RESOLVED);
            IncidentCloseRequestDTO dto = new IncidentCloseRequestDTO("Notas", null);

            when(incidentRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> service.close(1L, dto, 99L));
            assertTrue(ex.getMessage().contains("Usuario no encontrado"));
        }
    }

    // =========================================================================
    // buildFolio() — método estático, accesible desde el mismo paquete
    // =========================================================================

    @Nested
    class BuildFolio {

        @Test
        void should_returnFolioMatchingPattern_when_calledWithValidId() {
            String folio = IncidentServiceImpl.buildFolio(42L);
            // El año proviene de Year.now(), por eso se valida el patrón y no el valor exacto
            assertThat(folio).matches("INC-\\d{4}-00042");
        }

        @Test
        void should_padIdWithLeadingZeros_when_idIsSingleDigit() {
            String folio = IncidentServiceImpl.buildFolio(1L);
            assertThat(folio).endsWith("-00001");
        }

        @Test
        void should_handleLargeId_when_idExceedsFivePaddingDigits() {
            // IDs mayores a 99999 deben mostrarse sin truncar (formato %05d no trunca)
            String folio = IncidentServiceImpl.buildFolio(100000L);
            assertThat(folio).endsWith("-100000");
        }
    }

    // =========================================================================
    // extractIdFromFolio() — método estático, accesible desde el mismo paquete
    // =========================================================================

    @Nested
    class ExtractIdFromFolio {

        @Test
        void should_return42_when_folioIsValidFormat() {
            assertThat(IncidentServiceImpl.extractIdFromFolio("INC-2026-00042"))
                    .isEqualTo(42L);
        }

        @Test
        void should_ignoreYear_when_folioHasDifferentYear() {
            // Un admin puede buscar INC-2025-00001 estando en 2026 — el año se ignora
            assertThat(IncidentServiceImpl.extractIdFromFolio("INC-2025-00001"))
                    .isEqualTo(1L);
        }

        @Test
        void should_beCaseInsensitive_when_folioIsLowercase() {
            assertThat(IncidentServiceImpl.extractIdFromFolio("inc-2026-00042"))
                    .isEqualTo(42L);
        }

        @Test
        void should_trimWhitespace_when_folioHasLeadingTrailingSpaces() {
            assertThat(IncidentServiceImpl.extractIdFromFolio("  INC-2026-00042  "))
                    .isEqualTo(42L);
        }

        @Test
        void should_returnNull_when_folioIsNull() {
            assertThat(IncidentServiceImpl.extractIdFromFolio(null)).isNull();
        }

        @Test
        void should_returnNull_when_folioIsBlank() {
            assertThat(IncidentServiceImpl.extractIdFromFolio("   ")).isNull();
        }

        @Test
        void should_returnNull_when_folioHasInvalidPrefix() {
            assertThat(IncidentServiceImpl.extractIdFromFolio("FOO-2026-00042")).isNull();
        }

        @Test
        void should_returnNull_when_yearIsNotFourDigits() {
            // \d{4} requiere exactamente 4 dígitos
            assertThat(IncidentServiceImpl.extractIdFromFolio("INC-26-00042")).isNull();
        }

        @Test
        void should_returnNull_when_numericPartIsAlphanumeric() {
            assertThat(IncidentServiceImpl.extractIdFromFolio("INC-2026-ABCDE")).isNull();
        }

        @Test
        void should_return0_when_folioHasZeroId() {
            // "00000" → 0L — no hay incidencia con id 0, pero el parser no lo rechaza
            assertThat(IncidentServiceImpl.extractIdFromFolio("INC-2026-00000"))
                    .isZero();
        }
    }

    // =========================================================================
    // Helper de test
    // =========================================================================

    /**
     * Construye una AssetAssignment activa (returnedAt = null) vinculada al bien del fixture.
     */
    private AssetAssignment buildActiveAssignment() {
        AssetAssignment assignment = new AssetAssignment();
        assignment.setId(10L);
        assignment.setAsset(asset);
        assignment.setAssignedAt(LocalDateTime.of(2026, 1, 1, 8, 0));
        // returnedAt = null → asignación activa
        return assignment;
    }
}