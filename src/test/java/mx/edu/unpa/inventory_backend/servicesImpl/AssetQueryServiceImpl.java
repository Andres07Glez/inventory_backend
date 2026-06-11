package mx.edu.unpa.inventory_backend.servicesImpl;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.asset_assignment.response.AssignmentHistoryResponse;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianSummary;
import mx.edu.unpa.inventory_backend.enums.Campus;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.AssetMapper;
import mx.edu.unpa.inventory_backend.repositories.AssetAssignmentRepository;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.services.impl.AssetQueryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetQueryServiceImpl")
class AssetQueryServiceImplTest {

    // -----------------------------------------------------------------------
    // Collaborators
    // -----------------------------------------------------------------------
    @Mock private AssetRepository           assetRepository;
    @Mock private AssetAssignmentRepository assignmentRepository;
    @Mock private AssetMapper               assetMapper;

    @InjectMocks
    private AssetQueryServiceImpl sut;

    // -----------------------------------------------------------------------
    // Shared fixtures
    // -----------------------------------------------------------------------
    private Asset           asset;
    private AssetAssignment activeAssignment;
    private Guardian        guardian;

    @BeforeEach
    void buildFixtures() {
        guardian         = buildGuardian(10L, "María López", "EMP-001", "TI");
        asset            = buildAsset(1L, "INV-001", "BAR-001");
        activeAssignment = buildAssignment(100L, asset, guardian, null);
    }

    // =======================================================================
    // findByCode
    // =======================================================================
    @Nested
    @DisplayName("findByCode")
    class FindByCode {

        @Test
        @DisplayName("should_returnDetail_when_codeMatchesBarcode")
        void should_returnDetail_when_codeMatchesBarcode() {
            // Arrange
            AssetDetailResponse expected = buildDetailResponse(1L, null);

            when(assetRepository.findByBarcodeWithDetails("BAR-001"))
                    .thenReturn(Optional.of(asset));
            when(assignmentRepository.findActiveByAssetId(1L))
                    .thenReturn(Optional.empty());
            when(assetMapper.toDetailResponse(asset, null))
                    .thenReturn(expected);

            // Act
            AssetDetailResponse result = sut.findByCode("BAR-001");

            // Assert
            assertNotNull(result);
            // Si el barcode tiene match, NO debe intentar buscar por inventoryNumber
            verify(assetRepository, never()).findByInventoryNumberWithDetails(anyString());
        }

        @Test
        @DisplayName("should_returnDetail_when_codeMatchesInventoryNumberFallback")
        void should_returnDetail_when_codeMatchesInventoryNumberFallback() {
            // Arrange — barcode no encontrado, fallback a inventoryNumber
            AssetDetailResponse expected = buildDetailResponse(1L, null);

            when(assetRepository.findByBarcodeWithDetails("INV-001"))
                    .thenReturn(Optional.empty());
            when(assetRepository.findByInventoryNumberWithDetails("INV-001"))
                    .thenReturn(Optional.of(asset));
            when(assignmentRepository.findActiveByAssetId(1L))
                    .thenReturn(Optional.empty());
            when(assetMapper.toDetailResponse(asset, null))
                    .thenReturn(expected);

            // Act
            AssetDetailResponse result = sut.findByCode("INV-001");

            // Assert
            assertNotNull(result);
            verify(assetRepository, times(1)).findByBarcodeWithDetails("INV-001");
            verify(assetRepository, times(1)).findByInventoryNumberWithDetails("INV-001");
        }

        @Test
        @DisplayName("should_trimCode_before_querying_when_codeHasLeadingOrTrailingSpaces")
        void should_trimCode_before_querying_when_codeHasLeadingOrTrailingSpaces() {
            // Arrange — el service hace trim(), los repos deben recibir el código limpio
            AssetDetailResponse expected = buildDetailResponse(1L, null);

            when(assetRepository.findByBarcodeWithDetails("BAR-001"))
                    .thenReturn(Optional.of(asset));
            when(assignmentRepository.findActiveByAssetId(1L))
                    .thenReturn(Optional.empty());
            when(assetMapper.toDetailResponse(asset, null))
                    .thenReturn(expected);

            // Act
            sut.findByCode("  BAR-001  ");

            // Assert — los repositorios reciben el valor sin espacios
            verify(assetRepository, times(1)).findByBarcodeWithDetails("BAR-001");
            verify(assetRepository, never()).findByBarcodeWithDetails("  BAR-001  ");
        }

        @Test
        @DisplayName("should_returnDetailWithGuardian_when_activeAssignmentExists")
        void should_returnDetailWithGuardian_when_activeAssignmentExists() {
            // Arrange
            GuardianSummary guardianSummary = new GuardianSummary(10L, "María López", "EMP-001", "TI");
            AssetDetailResponse expected = buildDetailResponse(1L, guardianSummary);

            when(assetRepository.findByBarcodeWithDetails("BAR-001"))
                    .thenReturn(Optional.of(asset));
            when(assignmentRepository.findActiveByAssetId(1L))
                    .thenReturn(Optional.of(activeAssignment));
            when(assetMapper.toDetailResponse(asset, activeAssignment))
                    .thenReturn(expected);

            // Act
            AssetDetailResponse result = sut.findByCode("BAR-001");

            // Assert
            assertNotNull(result.guardian());
            assertEquals("María López", result.guardian().fullName());
            verify(assetMapper, times(1)).toDetailResponse(asset, activeAssignment);
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_neitherBarcodeNorInventoryNumberMatches")
        void should_throwResourceNotFoundException_when_neitherBarcodeNorInventoryNumberMatches() {
            // Arrange
            when(assetRepository.findByBarcodeWithDetails("UNKNOWN"))
                    .thenReturn(Optional.empty());
            when(assetRepository.findByInventoryNumberWithDetails("UNKNOWN"))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> sut.findByCode("UNKNOWN"));

            verify(assignmentRepository, never()).findActiveByAssetId(anyLong());
            verify(assetMapper,          never()).toDetailResponse(any(), any());
        }

        // Edge case: asignación activa con guardian null (datos corruptos en BD)
        @Test
        @DisplayName("should_returnNullGuardian_when_activeAssignmentHasNullGuardian")
        void should_returnNullGuardian_when_activeAssignmentHasNullGuardian() {
            // Arrange
            AssetAssignment corruptAssignment = buildAssignment(101L, asset, null, null);
            AssetDetailResponse expected = buildDetailResponse(1L, null);

            when(assetRepository.findByBarcodeWithDetails("BAR-001"))
                    .thenReturn(Optional.of(asset));
            when(assignmentRepository.findActiveByAssetId(1L))
                    .thenReturn(Optional.of(corruptAssignment));
            when(assetMapper.toDetailResponse(asset, corruptAssignment))
                    .thenReturn(expected);

            // Act
            AssetDetailResponse result = sut.findByCode("BAR-001");

            // Assert — el mapper maneja null internamente; el service no debe explotar
            assertNull(result.guardian());
        }
    }

    // =======================================================================
    // findById
    // =======================================================================
    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("should_returnDetail_when_assetExistsWithActiveAssignment")
        void should_returnDetail_when_assetExistsWithActiveAssignment() {
            // Arrange
            GuardianSummary guardianSummary = new GuardianSummary(10L, "María López", "EMP-001", "TI");
            AssetDetailResponse expected = buildDetailResponse(1L, guardianSummary);

            when(assetRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(asset));
            when(assignmentRepository.findActiveByAssetId(1L))
                    .thenReturn(Optional.of(activeAssignment));
            when(assetMapper.toDetailResponse(asset, activeAssignment))
                    .thenReturn(expected);

            // Act
            AssetDetailResponse result = sut.findById(1L);

            // Assert
            assertNotNull(result);
            assertNotNull(result.guardian());
            verify(assetRepository,       times(1)).findByIdWithDetails(1L);
            verify(assignmentRepository,  times(1)).findActiveByAssetId(1L);
        }

        @Test
        @DisplayName("should_returnDetailWithNullGuardian_when_assetHasNoActiveAssignment")
        void should_returnDetailWithNullGuardian_when_assetHasNoActiveAssignment() {
            // Arrange
            AssetDetailResponse expected = buildDetailResponse(1L, null);

            when(assetRepository.findByIdWithDetails(1L))
                    .thenReturn(Optional.of(asset));
            when(assignmentRepository.findActiveByAssetId(1L))
                    .thenReturn(Optional.empty());
            when(assetMapper.toDetailResponse(asset, null))
                    .thenReturn(expected);

            // Act
            AssetDetailResponse result = sut.findById(1L);

            // Assert
            assertNull(result.guardian());
            verify(assetMapper, times(1)).toDetailResponse(asset, null);
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assetDoesNotExist")
        void should_throwResourceNotFoundException_when_assetDoesNotExist() {
            // Arrange
            when(assetRepository.findByIdWithDetails(99L))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> sut.findById(99L));

            verify(assignmentRepository, never()).findActiveByAssetId(anyLong());
            verify(assetMapper,          never()).toDetailResponse(any(), any());
        }
    }

    // =======================================================================
    // findAssignmentHistory
    // =======================================================================
    @Nested
    @DisplayName("findAssignmentHistory")
    class FindAssignmentHistory {

        @Test
        @DisplayName("should_returnFullHistory_when_assetHasMultipleAssignments")
        void should_returnFullHistory_when_assetHasMultipleAssignments() {
            // Arrange
            AssetAssignment closedAssignment = buildAssignment(
                    200L, asset, guardian,
                    LocalDateTime.of(2024, 1, 15, 10, 0));

            List<AssetAssignment> allAssignments = List.of(closedAssignment, activeAssignment);

            AssignmentHistoryResponse closedResponse = buildHistoryResponse(
                    200L, "María López", LocalDateTime.of(2024, 1, 15, 10, 0));
            AssignmentHistoryResponse activeResponse = buildHistoryResponse(100L, "María López", null);
            List<AssignmentHistoryResponse> expected = List.of(closedResponse, activeResponse);

            when(assetRepository.existsById(1L)).thenReturn(true);
            when(assignmentRepository.findAllByAssetIdOrderByActivity(1L))
                    .thenReturn(allAssignments);
            when(assetMapper.toAssignmentHistoryResponseList(allAssignments))
                    .thenReturn(expected);

            // Act
            List<AssignmentHistoryResponse> result = sut.findAssignmentHistory(1L);

            // Assert
            assertEquals(2, result.size());
            assertNotNull(result.get(0).returnedAt(), "La asignación cerrada debe tener fecha de devolución");
            assertNull(result.get(1).returnedAt(),    "La asignación activa no debe tener fecha de devolución");
        }

        @Test
        @DisplayName("should_returnEmptyList_when_assetHasNoAssignments")
        void should_returnEmptyList_when_assetHasNoAssignments() {
            // Arrange
            when(assetRepository.existsById(1L)).thenReturn(true);
            when(assignmentRepository.findAllByAssetIdOrderByActivity(1L))
                    .thenReturn(Collections.emptyList());
            when(assetMapper.toAssignmentHistoryResponseList(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            // Act
            List<AssignmentHistoryResponse> result = sut.findAssignmentHistory(1L);

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assetDoesNotExist")
        void should_throwResourceNotFoundException_when_assetDoesNotExist() {
            // Arrange
            when(assetRepository.existsById(99L)).thenReturn(false);

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> sut.findAssignmentHistory(99L));

            verify(assignmentRepository, never()).findAllByAssetIdOrderByActivity(anyLong());
            verify(assetMapper,          never()).toAssignmentHistoryResponseList(any());
        }
    }

    // =======================================================================
    // Private builders
    // =======================================================================

    private Asset buildAsset(Long id, String inventoryNumber, String barcode) {
        Category category = new Category();
        category.setId(1);
        category.setName("Computadoras");

        Asset a = new Asset();
        a.setId(id);
        a.setInventoryNumber(inventoryNumber);
        a.setBarcode(barcode);
        a.setDescription("Laptop Dell Latitude");
        a.setCategory(category);
        a.setConditionStatus(ConditionStatus.GOOD);
        a.setLifecycleStatus(LifecycleStatus.AVAILABLE);
        a.setEntryDate(LocalDate.of(2023, 6, 1));
        return a;
    }

    private Guardian buildGuardian(Long id, String fullName, String employeeNumber, String department) {
        Guardian g = new Guardian();
        g.setId(id);
        g.setFullName(fullName);
        g.setEmployeeNumber(employeeNumber);
        g.setDepartment(department);
        return g;
    }

    private AssetAssignment buildAssignment(Long id, Asset asset, Guardian guardian, LocalDateTime returnedAt) {
        AssetAssignment aa = new AssetAssignment();
        aa.setId(id);
        aa.setAsset(asset);
        aa.setGuardian(guardian);
        aa.setAssignedAt(LocalDateTime.of(2024, 3, 1, 9, 0));
        aa.setReturnedAt(returnedAt);
        return aa;
    }

    private AssetDetailResponse buildDetailResponse(Long id, GuardianSummary guardian) {
        return new AssetDetailResponse(
                id,
                "INV-001",
                "BAR-001",
                "Laptop Dell Latitude",
                "Dell",
                "Latitude 5520",
                "SN-123",
                "Computadoras",
                "Laboratorio A",
                "Edificio 3",
                Campus.LOMA_BONITA,
                ConditionStatus.GOOD,
                LifecycleStatus.ASSIGNED,
                LocalDate.of(2023, 6, 1),
                LocalDateTime.of(2024, 3, 1, 9, 0),
                guardian,
                Collections.emptyList()
        );
    }

    private AssignmentHistoryResponse buildHistoryResponse(Long id, String guardianName, LocalDateTime returnedAt) {
        return new AssignmentHistoryResponse(
                id,
                guardianName,
                "EMP-001",
                "Laboratorio A",
                LocalDateTime.of(2024, 3, 1, 9, 0),
                returnedAt,
                "admin",
                null
        );
    }
}