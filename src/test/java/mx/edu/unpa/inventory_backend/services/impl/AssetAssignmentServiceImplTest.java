package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetAssignmentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetAssignmentResponseDTO;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.AssetAssignmentMapper;
import mx.edu.unpa.inventory_backend.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetAssignmentServiceImpl")
class AssetAssignmentServiceImplTest {

    // ─── Mocks ──────────────────────────────────────────────────────────────────
    @Mock private AssetAssignmentRepository assignmentRepository;
    @Mock private AssetRepository           assetRepository;
    @Mock private GuardianRepository        guardianRepository;
    @Mock private UserRepository            userRepository;
    @Mock private AssetAssignmentMapper     mapper;

    @InjectMocks
    private AssetAssignmentServiceImpl service;

    // ─── Fixtures base ──────────────────────────────────────────────────────────
    private static final Long ASSET_ID      = 10L;
    private static final Long GUARDIAN_ID   = 20L;
    private static final Long ASSIGNED_BY   = 1L;

    private Asset       asset;
    private Guardian    guardian;
    private Location    location;
    private User        assignedBy;
    private AssetAssignmentRequestDTO request;

    @BeforeEach
    void setUp() {
        location = new Location();
        location.setId(5);
        location.setName("Edificio A - Planta Baja");

        asset = new Asset();
        asset.setId(ASSET_ID);
        asset.setLifecycleStatus(LifecycleStatus.AVAILABLE);

        guardian = new Guardian();
        guardian.setId(GUARDIAN_ID);
        guardian.setFullName("Juan Pérez");
        guardian.setLocation(location);

        assignedBy = User.builder()
                .id(ASSIGNED_BY)
                .username("admin")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        request = new AssetAssignmentRequestDTO(ASSET_ID, GUARDIAN_ID, "Notas de prueba");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HAPPY PATH
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Happy path — assignAsset")
    class HappyPath {

        @Test
        @DisplayName("should_returnResponseDTO_when_allEntitiesAreValidAndNoActiveAssignmentExists")
        void should_returnResponseDTO_when_allEntitiesAreValidAndNoActiveAssignmentExists() {
            // Arrange
            AssetAssignment savedAssignment = buildSavedAssignment();
            AssetAssignmentResponseDTO expectedDto = mock(AssetAssignmentResponseDTO.class);

            stubHappyPath(savedAssignment, expectedDto);
            when(assignmentRepository.findActiveByAssetId(ASSET_ID)).thenReturn(Optional.empty());

            // Act
            AssetAssignmentResponseDTO result = service.assignAsset(request, ASSIGNED_BY);

            // Assert
            assertThat(result).isEqualTo(expectedDto);
        }

        @Test
        @DisplayName("should_setAssetStatusToAssigned_when_assignmentIsCreated")
        void should_setAssetStatusToAssigned_when_assignmentIsCreated() {
            // Arrange
            stubHappyPath(buildSavedAssignment(), mock(AssetAssignmentResponseDTO.class));
            when(assignmentRepository.findActiveByAssetId(ASSET_ID)).thenReturn(Optional.empty());

            // Act
            service.assignAsset(request, ASSIGNED_BY);

            // Assert
            ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
            verify(assetRepository).save(assetCaptor.capture());
            assertThat(assetCaptor.getValue().getLifecycleStatus()).isEqualTo(LifecycleStatus.ASSIGNED);
        }

        @Test
        @DisplayName("should_inheritGuardianLocation_when_assetIsSaved")
        void should_inheritGuardianLocation_when_assetIsSaved() {
            // Arrange
            stubHappyPath(buildSavedAssignment(), mock(AssetAssignmentResponseDTO.class));
            when(assignmentRepository.findActiveByAssetId(ASSET_ID)).thenReturn(Optional.empty());

            // Act
            service.assignAsset(request, ASSIGNED_BY);

            // Assert
            ArgumentCaptor<Asset> assetCaptor = ArgumentCaptor.forClass(Asset.class);
            verify(assetRepository).save(assetCaptor.capture());
            assertThat(assetCaptor.getValue().getLocation()).isEqualTo(location);
        }

        @Test
        @DisplayName("should_persistNewAssignmentWithCorrectFields_when_requestIsValid")
        void should_persistNewAssignmentWithCorrectFields_when_requestIsValid() {
            // Arrange
            stubHappyPath(buildSavedAssignment(), mock(AssetAssignmentResponseDTO.class));
            when(assignmentRepository.findActiveByAssetId(ASSET_ID)).thenReturn(Optional.empty());

            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // Act
            service.assignAsset(request, ASSIGNED_BY);

            // Assert
            ArgumentCaptor<AssetAssignment> captor = ArgumentCaptor.forClass(AssetAssignment.class);
            verify(assignmentRepository).save(captor.capture());
            AssetAssignment saved = captor.getValue();

            assertThat(saved.getAsset()).isEqualTo(asset);
            assertThat(saved.getGuardian()).isEqualTo(guardian);
            assertThat(saved.getLocation()).isEqualTo(location);
            assertThat(saved.getAssignedBy()).isEqualTo(assignedBy);
            assertThat(saved.getNotes()).isEqualTo("Notas de prueba");
            assertThat(saved.getAssignedAt()).isAfterOrEqualTo(before);
            assertThat(saved.getReturnedAt()).isNull();
        }

        @Test
        @DisplayName("should_persistAssignmentWithNullNotes_when_notesFieldIsNull")
        void should_persistAssignmentWithNullNotes_when_notesFieldIsNull() {
            // Arrange — notes = null (campo opcional en el DTO)
            request = new AssetAssignmentRequestDTO(ASSET_ID, GUARDIAN_ID, null);
            stubHappyPath(buildSavedAssignment(), mock(AssetAssignmentResponseDTO.class));
            when(assignmentRepository.findActiveByAssetId(ASSET_ID)).thenReturn(Optional.empty());

            // Act
            service.assignAsset(request, ASSIGNED_BY);

            // Assert
            ArgumentCaptor<AssetAssignment> captor = ArgumentCaptor.forClass(AssetAssignment.class);
            verify(assignmentRepository).save(captor.capture());
            assertThat(captor.getValue().getNotes()).isNull();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CIERRE DE ASIGNACIÓN ACTIVA PREVIA
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Active assignment closure")
    class ActiveAssignmentClosure {

        @Test
        @DisplayName("should_closeExistingActiveAssignment_when_assetAlreadyHasOneActiveAssignment")
        void should_closeExistingActiveAssignment_when_assetAlreadyHasOneActiveAssignment() {
            // Arrange
            AssetAssignment existingActive = new AssetAssignment();
            existingActive.setId(99L);
            existingActive.setReturnedAt(null); // activa

            stubHappyPath(buildSavedAssignment(), mock(AssetAssignmentResponseDTO.class));
            when(assignmentRepository.findActiveByAssetId(ASSET_ID))
                    .thenReturn(Optional.of(existingActive));

            LocalDateTime before = LocalDateTime.now().minusSeconds(1);

            // Act
            service.assignAsset(request, ASSIGNED_BY);

            // Assert: se persiste la asignación anterior con returnedAt seteado
            ArgumentCaptor<AssetAssignment> captor = ArgumentCaptor.forClass(AssetAssignment.class);
            // primer save = cierre de la activa; segundo save = nueva asignación
            verify(assignmentRepository, times(2)).save(captor.capture());

            AssetAssignment closed = captor.getAllValues().get(0);
            assertThat(closed.getId()).isEqualTo(99L);
            assertThat(closed.getReturnedAt()).isAfterOrEqualTo(before);
        }

        @Test
        @DisplayName("should_saveNewAssignment_even_when_closingPreviousActiveAssignment")
        void should_saveNewAssignment_even_when_closingPreviousActiveAssignment() {
            // Arrange
            AssetAssignment existingActive = new AssetAssignment();
            existingActive.setId(99L);

            AssetAssignment savedNew = buildSavedAssignment();
            AssetAssignmentResponseDTO expectedDto = mock(AssetAssignmentResponseDTO.class);

            stubHappyPath(savedNew, expectedDto);
            when(assignmentRepository.findActiveByAssetId(ASSET_ID))
                    .thenReturn(Optional.of(existingActive));

            // Act
            AssetAssignmentResponseDTO result = service.assignAsset(request, ASSIGNED_BY);

            // Assert
            assertThat(result).isEqualTo(expectedDto);
            verify(assignmentRepository, times(2)).save(any(AssetAssignment.class));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASSET — ERRORES
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Asset validation errors")
    class AssetValidation {

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assetDoesNotExist")
        void should_throwResourceNotFoundException_when_assetDoesNotExist() {
            // Arrange
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ASSET_ID));
        }

        @Test
        @DisplayName("should_throwIllegalStateException_when_assetIsDecommissioned")
        void should_throwIllegalStateException_when_assetIsDecommissioned() {
            // Arrange
            asset.setLifecycleStatus(LifecycleStatus.DECOMMISSIONED);
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

            // Act & Assert
            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("dado de baja");
        }

        @Test
        @DisplayName("should_allowAssignment_when_assetIsInMaintenanceStatus")
        void should_allowAssignment_when_assetIsInMaintenanceStatus() {
            // Arrange — IN_MAINTENANCE no está bloqueado en el service actual
            asset.setLifecycleStatus(LifecycleStatus.IN_MAINTENANCE);
            stubHappyPath(buildSavedAssignment(), mock(AssetAssignmentResponseDTO.class));
            when(assignmentRepository.findActiveByAssetId(ASSET_ID)).thenReturn(Optional.empty());

            // Act & Assert: no debe lanzar excepción
            assertThatNoException().isThrownBy(() -> service.assignAsset(request, ASSIGNED_BY));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GUARDIAN — ERRORES
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Guardian validation errors")
    class GuardianValidation {

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_guardianDoesNotExist")
        void should_throwResourceNotFoundException_when_guardianDoesNotExist() {
            // Arrange
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(guardianRepository.findById(GUARDIAN_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(GUARDIAN_ID));
        }

        @Test
        @DisplayName("should_throwIllegalStateException_when_guardianHasNoBaseLocation")
        void should_throwIllegalStateException_when_guardianHasNoBaseLocation() {
            // Arrange — guardián sin ubicación base
            guardian.setLocation(null);
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(guardianRepository.findById(GUARDIAN_ID)).thenReturn(Optional.of(guardian));

            // Act & Assert
            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(guardian.getFullName())
                    .hasMessageContaining("ubicación base");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // USER — ERRORES
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("AssignedBy user validation errors")
    class AssignedByUserValidation {

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assignedByUserDoesNotExist")
        void should_throwResourceNotFoundException_when_assignedByUserDoesNotExist() {
            // Arrange
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(guardianRepository.findById(GUARDIAN_ID)).thenReturn(Optional.of(guardian));
            when(userRepository.findByIdAndIsActiveTrue(ASSIGNED_BY)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ASSIGNED_BY));
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assignedByUserIsInactive")
        void should_throwResourceNotFoundException_when_assignedByUserIsInactive() {
            // El repositorio usa findByIdAndIsActiveTrue, así que un usuario inactivo
            // devuelve Optional.empty() — mismo comportamiento que no encontrado.
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(guardianRepository.findById(GUARDIAN_ID)).thenReturn(Optional.of(guardian));
            when(userRepository.findByIdAndIsActiveTrue(ASSIGNED_BY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VERIFICACIONES DE INTERACCIÓN
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Repository interaction verifications")
    class RepositoryInteractions {

        @Test
        @DisplayName("should_neverSaveAsset_when_assetNotFound")
        void should_neverSaveAsset_when_assetNotFound() {
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(assetRepository, never()).save(any());
            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_neverSaveAsset_when_assetIsDecommissioned")
        void should_neverSaveAsset_when_assetIsDecommissioned() {
            asset.setLifecycleStatus(LifecycleStatus.DECOMMISSIONED);
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));

            assertThatThrownBy(() -> service.assignAsset(request, ASSIGNED_BY))
                    .isInstanceOf(IllegalStateException.class);

            verify(assetRepository, never()).save(any());
            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_callMapperWithPersistedAssignment_when_assignmentIsSaved")
        void should_callMapperWithPersistedAssignment_when_assignmentIsSaved() {
            // Arrange
            AssetAssignment savedAssignment = buildSavedAssignment();
            AssetAssignmentResponseDTO expectedDto = mock(AssetAssignmentResponseDTO.class);

            stubHappyPath(savedAssignment, expectedDto);
            when(assignmentRepository.findActiveByAssetId(ASSET_ID)).thenReturn(Optional.empty());

            // Act
            service.assignAsset(request, ASSIGNED_BY);

            // Assert: el mapper recibe exactamente lo que devuelve el repository.save
            verify(mapper).toDto(savedAssignment);
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Configura los stubs comunes del happy path.
     * Se separa para no repetir el mismo bloque de when() en cada test.
     */
    private void stubHappyPath(AssetAssignment savedAssignment,
                               AssetAssignmentResponseDTO dto) {
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(guardianRepository.findById(GUARDIAN_ID)).thenReturn(Optional.of(guardian));
        when(userRepository.findByIdAndIsActiveTrue(ASSIGNED_BY)).thenReturn(Optional.of(assignedBy));
        when(assignmentRepository.save(any(AssetAssignment.class))).thenReturn(savedAssignment);
        when(mapper.toDto(savedAssignment)).thenReturn(dto);
    }

    /** Construye una instancia de AssetAssignment que simula el retorno del repositorio. */
    private AssetAssignment buildSavedAssignment() {
        AssetAssignment a = new AssetAssignment();
        a.setId(100L);
        a.setAsset(asset);
        a.setGuardian(guardian);
        a.setLocation(location);
        a.setAssignedBy(assignedBy);
        a.setAssignedAt(LocalDateTime.now());
        return a;
    }
}