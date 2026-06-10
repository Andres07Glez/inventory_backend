package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.decommission.request.DecommissionRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionResponseDTO;
import mx.edu.unpa.inventory_backend.dtos.decommission.response.DecommissionSummaryDTO;
import mx.edu.unpa.inventory_backend.enums.*;
import mx.edu.unpa.inventory_backend.exceptions.InvalidDecommissionStateException;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecommissionServiceImplTest {

    // ── Dependencias mockeadas ────────────────────────────────────────────────

    @Mock private DecommissionRepository decommissionRepository;
    @Mock private AssetRepository        assetRepository;
    @Mock private IncidentRepository     incidentRepository;
    @Mock private UserRepository         userRepository;
    @Mock private StorageService         storageService;

    @InjectMocks
    private DecommissionServiceImpl service;

    // ── Fixtures compartidos ──────────────────────────────────────────────────

    private Asset            asset;
    private User             operator;
    private User             admin;
    private Incident         incident;
    private AssetDecommission pendingDecommission;

    @BeforeEach
    void setUp() {
        // Guardians (necesarios para toResponseDTO → createdBy.guardian.fullName)
        Guardian operatorGuardian = new Guardian();
        operatorGuardian.setId(1L);
        operatorGuardian.setFullName("Ana García");

        Guardian adminGuardian = new Guardian();
        adminGuardian.setId(2L);
        adminGuardian.setFullName("Carlos Reyes");

        operator = User.builder()
                .id(1L)
                .username("operador01")
                .passwordHash("$2a$hash")
                .role(UserRole.OPERADOR)
                .isActive(true)
                .guardian(operatorGuardian)
                .build();

        admin = User.builder()
                .id(2L)
                .username("admin01")
                .passwordHash("$2a$hash")
                .role(UserRole.ADMIN)
                .isActive(true)
                .guardian(adminGuardian)
                .build();

        Category category = new Category();
        category.setId(1);
        category.setName("Equipo de Cómputo");

        Brand brand = new Brand();
        brand.setId(1);
        brand.setName("Dell");

        asset = new Asset();
        asset.setId(10L);
        asset.setInventoryNumber("INV-2026-00010");
        asset.setDescription("Laptop Dell Latitude 5520");
        asset.setCategory(category);
        asset.setBrand(brand);
        asset.setEntryDate(LocalDate.of(2024, 1, 15));
        asset.setLifecycleStatus(LifecycleStatus.AVAILABLE);
        asset.setConditionStatus(ConditionStatus.GOOD);
        asset.setCreatedBy(operator);
        asset.setUpdatedBy(operator);

        incident = new Incident();
        incident.setId(5L);
        incident.setAsset(asset);
        incident.setStatus(IncidentStatus.CLOSED);
        incident.setDescription("Pantalla dañada por golpe");
        incident.setIncidentDate(LocalDate.of(2026, 1, 10));
        incident.setConditionAtIncident(ConditionStatus.BAD);
        incident.setCreatedBy(operator);

        pendingDecommission = new AssetDecommission();
        pendingDecommission.setId(1L);
        pendingDecommission.setAsset(asset);
        pendingDecommission.setIncident(null);
        pendingDecommission.setJustification("Bien fuera de servicio por obsolescencia");
        pendingDecommission.setDocumentPath(null);
        pendingDecommission.setDecommissionDate(LocalDate.of(2026, 6, 1));
        pendingDecommission.setStatus(DecommissionStatus.PENDING);
        pendingDecommission.setCreatedBy(operator);
        pendingDecommission.setCreatedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    // =========================================================================
    // create()
    // =========================================================================

    @Nested
    class Create {

        @Test
        void should_returnPendingDecommission_when_validRequestWithoutDocumentOrIncident()
                throws IOException {
            // Arrange
            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Bien fuera de servicio", LocalDate.of(2026, 6, 1));

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(decommissionRepository.save(any(AssetDecommission.class)))
                    .thenReturn(pendingDecommission);

            // Act
            DecommissionResponseDTO result = service.create(request, null, operator.getId());

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.assetId()).isEqualTo(asset.getId());
            assertThat(result.status()).isEqualTo(DecommissionStatus.PENDING);
            assertThat(result.incidentId()).isNull();
            assertThat(result.decommissionDocumentUrl()).isNull();
            verify(decommissionRepository).save(any(AssetDecommission.class));
            verify(storageService, never()).store(any(), anyString());
        }

        @Test
        void should_linkIncident_when_incidentIdBelongsToSameAsset() throws IOException {
            // Arrange
            AssetDecommission withIncident = buildDecommissionWith(pendingDecommission, incident);

            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), incident.getId(), "Baja por incidencia grave",
                    LocalDate.of(2026, 6, 1));

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
            when(decommissionRepository.save(any())).thenReturn(withIncident);

            // Act
            DecommissionResponseDTO result = service.create(request, null, operator.getId());

            // Assert
            assertThat(result.incidentId()).isEqualTo(incident.getId());
            // Folio debe tener el formato INC-YYYY-NNNNN
            assertThat(result.incidentFolio()).matches("INC-\\d{4}-\\d{5}");
        }

        @Test
        void should_useLocalDateNow_when_decommissionDateIsNull() {
            // Arrange
            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Baja sin fecha explícita", null);

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(decommissionRepository.save(any(AssetDecommission.class))).thenAnswer(inv -> {
                AssetDecommission saved = inv.getArgument(0);
                // Verifica el efecto secundario dentro del arrange
                assertThat(saved.getDecommissionDate()).isEqualTo(LocalDate.now());
                return pendingDecommission;
            });

            // Act & Assert — no debe lanzar excepción
            assertThatNoException().isThrownBy(() -> service.create(request, null, operator.getId()));
        }

        @Test
        void should_storeDocumentAndReturnUrl_when_validPdfIsProvided() throws IOException {
            // Arrange
            MultipartFile pdfFile = mock(MultipartFile.class);
            when(pdfFile.isEmpty()).thenReturn(false);
            when(pdfFile.getContentType()).thenReturn("application/pdf");
            when(pdfFile.getSize()).thenReturn(512 * 1024L); // 512 KB

            String storedPath = "decommissions/10/docs/acta_baja.pdf";
            String publicUrl  = "https://server/files/" + storedPath;

            AssetDecommission withDoc = new AssetDecommission();
            withDoc.setId(3L);
            withDoc.setAsset(asset);
            withDoc.setJustification("Bien fuera de servicio");
            withDoc.setDocumentPath(storedPath);
            withDoc.setDecommissionDate(LocalDate.of(2026, 6, 1));
            withDoc.setStatus(DecommissionStatus.PENDING);
            withDoc.setCreatedBy(operator);
            withDoc.setCreatedAt(LocalDateTime.now());

            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Bien fuera de servicio", LocalDate.of(2026, 6, 1));

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(storageService.store(eq(pdfFile), contains("decommissions/" + asset.getId())))
                    .thenReturn(storedPath);
            when(storageService.buildPublicUrl(storedPath)).thenReturn(publicUrl);
            when(decommissionRepository.save(any())).thenReturn(withDoc);

            // Act
            DecommissionResponseDTO result = service.create(request, pdfFile, operator.getId());

            // Assert
            assertThat(result.decommissionDocumentUrl()).isEqualTo(publicUrl);
            verify(storageService).store(eq(pdfFile), contains("decommissions/" + asset.getId()));
        }

        @Test
        void should_skipStorage_when_fileIsEmpty() throws IOException {
            // Arrange — archivo presente pero vacío (caso: form enviado sin adjunto)
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);

            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(decommissionRepository.save(any())).thenReturn(pendingDecommission);

            // Act
            service.create(request, emptyFile, operator.getId());

            // Assert
            verify(storageService, never()).store(any(), anyString());
        }

        // ── Edge cases: validaciones de negocio ──────────────────────────────

        @Test
        void should_throwResourceNotFoundException_when_assetNotFound() {
            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    999L, null, "Justificación", LocalDate.now());
            when(assetRepository.findById(999L)).thenReturn(Optional.empty());
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.create(request, null, operatorId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void should_throwInvalidDecommissionStateException_when_assetIsAlreadyDecommissioned() {
            asset.setLifecycleStatus(LifecycleStatus.DECOMMISSIONED);
            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.create(request, null, operatorId))
                    .isInstanceOf(InvalidDecommissionStateException.class)
                    .hasMessageContaining(asset.getInventoryNumber());
        }

        @Test
        void should_throwInvalidDecommissionStateException_when_assetAlreadyHasDecommissionProcess() {
            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(true);
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.create(request, null, operatorId))
                    .isInstanceOf(InvalidDecommissionStateException.class)
                    .hasMessageContaining(asset.getInventoryNumber());

            // El usuario nunca se consulta: el guard aborta antes
            verify(userRepository, never()).findByIdAndIsActiveTrue(anyLong());
        }

        @Test
        void should_throwResourceNotFoundException_when_createdByUserNotFound() {
            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(request, null, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void should_throwResourceNotFoundException_when_incidentNotFound() {
            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), 999L, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(incidentRepository.findById(999L)).thenReturn(Optional.empty());
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.create(request, null, operatorId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void should_throwResponseStatusException_when_incidentBelongsToDifferentAsset() {
            // Incidencia de otro bien — integrity edge case
            Asset otherAsset = new Asset();
            otherAsset.setId(99L);
            otherAsset.setInventoryNumber("INV-2026-99999");

            Incident foreignIncident = new Incident();
            foreignIncident.setId(20L);
            foreignIncident.setAsset(otherAsset);

            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), foreignIncident.getId(), "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(incidentRepository.findById(foreignIncident.getId()))
                    .thenReturn(Optional.of(foreignIncident));
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.create(request, null, operatorId))
                    .isInstanceOf(ResponseStatusException.class);
        }

        // ── Edge cases: validación de documento ──────────────────────────────

        @Test
        void should_throwUnsupportedMediaType_when_documentIsNotPdf() {
            MultipartFile imageFile = mock(MultipartFile.class);
            when(imageFile.isEmpty()).thenReturn(false);
            when(imageFile.getContentType()).thenReturn("image/jpeg");

            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.create(request, imageFile, operatorId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("PDF");
        }

        @Test
        void should_throwContentTooLarge_when_documentExceeds20Mb() {
            // Arrange — 21 MB supera el límite configurado de 20 MB
            MultipartFile oversized = mock(MultipartFile.class);
            when(oversized.isEmpty()).thenReturn(false);
            when(oversized.getContentType()).thenReturn("application/pdf");
            when(oversized.getSize()).thenReturn(21 * 1024 * 1024L);

            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.create(request, oversized, operatorId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("20 MB");
        }

        @Test
        void should_acceptDocument_when_sizeIsExactly20Mb() {
            // Boundary: exactamente en el límite (no debe lanzar)
            MultipartFile exactLimit = mock(MultipartFile.class);
            when(exactLimit.isEmpty()).thenReturn(false);
            when(exactLimit.getContentType()).thenReturn("application/pdf");
            when(exactLimit.getSize()).thenReturn(20 * 1024 * 1024L); // == límite

            DecommissionRequestDTO request = new DecommissionRequestDTO(
                    asset.getId(), null, "Justificación", LocalDate.now());

            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.existsByAssetId(asset.getId())).thenReturn(false);
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(storageService.store(any(), anyString()))
                    .thenReturn("decommissions/10/docs/acta.pdf");
            when(decommissionRepository.save(any())).thenReturn(pendingDecommission);

            // Act & Assert — el boundary exacto debe pasar
            assertThatNoException().isThrownBy(
                    () -> service.create(request, exactLimit, operator.getId()));
        }
    }

    // =========================================================================
    // confirm()
    // =========================================================================

    @Nested
    class Confirm {

        @Test
        void should_confirmDecommission_and_markAssetDecommissioned_when_statusIsPending() {
            // Arrange
            when(decommissionRepository.findByIdWithDetails(pendingDecommission.getId()))
                    .thenReturn(Optional.of(pendingDecommission));
            when(userRepository.findByIdAndIsActiveTrue(admin.getId()))
                    .thenReturn(Optional.of(admin));
            when(assetRepository.save(asset)).thenReturn(asset);
            when(decommissionRepository.save(pendingDecommission)).thenReturn(pendingDecommission);

            // Act
            DecommissionResponseDTO result =
                    service.confirm(pendingDecommission.getId(), admin.getId());

            // Assert — se deben producir AMBOS efectos dentro de la misma transacción
            assertThat(result).isNotNull();
            assertThat(asset.getLifecycleStatus())
                    .isEqualTo(LifecycleStatus.DECOMMISSIONED);
            assertThat(pendingDecommission.getStatus())
                    .isEqualTo(DecommissionStatus.CONFIRMED);
            assertThat(pendingDecommission.getConfirmedBy()).isEqualTo(admin);
            assertThat(pendingDecommission.getConfirmedAt()).isNotNull();
            verify(assetRepository).save(asset);
            verify(decommissionRepository).save(pendingDecommission);
        }

        @Test
        void should_throwInvalidDecommissionStateException_when_alreadyConfirmed() {
            // Este guard evita confirmar dos veces la misma baja
            pendingDecommission.setStatus(DecommissionStatus.CONFIRMED);

            when(decommissionRepository.findByIdWithDetails(pendingDecommission.getId()))
                    .thenReturn(Optional.of(pendingDecommission));

            Long decommissionId = pendingDecommission.getId();
            Long adminId = admin.getId();

            assertThatThrownBy(() -> service.confirm(decommissionId, adminId))
                    .isInstanceOf(InvalidDecommissionStateException.class)
                    .hasMessageContaining(String.valueOf(decommissionId));

            // El bien no debe modificarse si ya está confirmado
            verify(assetRepository, never()).save(any());
        }

        @Test
        void should_throwResourceNotFoundException_when_decommissionNotFound() {
            when(decommissionRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());
            Long adminId = admin.getId();
            assertThatThrownBy(() -> service.confirm(999L, adminId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        void should_throwResourceNotFoundException_when_adminUserNotFound() {
            when(decommissionRepository.findByIdWithDetails(pendingDecommission.getId()))
                    .thenReturn(Optional.of(pendingDecommission));
            when(userRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

            Long pendigDecommissionId = pendingDecommission.getId();
            assertThatThrownBy(() -> service.confirm(pendigDecommissionId, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(assetRepository, never()).save(any());
        }
    }

    // =========================================================================
    // getById()
    // =========================================================================

    @Nested
    class GetById {

        @Test
        void should_returnResponseDTO_when_idExists() {
            when(decommissionRepository.findByIdWithDetails(pendingDecommission.getId()))
                    .thenReturn(Optional.of(pendingDecommission));

            DecommissionResponseDTO result = service.getById(pendingDecommission.getId());

            assertThat(result.id()).isEqualTo(pendingDecommission.getId());
            assertThat(result.assetInventoryNumber()).isEqualTo(asset.getInventoryNumber());
            assertThat(result.createdByName()).isEqualTo(
                    operator.getGuardian().getFullName());
        }

        @Test
        void should_throwResourceNotFoundException_when_idNotFound() {
            when(decommissionRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // list()
    // =========================================================================

    @Nested
    class List {

        @Test
        void should_returnPagedSummaries_when_filterByStatus() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<AssetDecommission> page =
                    new PageImpl<>(java.util.List.of(pendingDecommission), pageable, 1);

            when(decommissionRepository.findAllFiltered(DecommissionStatus.PENDING, pageable))
                    .thenReturn(page);

            Page<DecommissionSummaryDTO> result =
                    service.list(DecommissionStatus.PENDING, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            DecommissionSummaryDTO summary = result.getContent().get(0);
            assertThat(summary.status()).isEqualTo(DecommissionStatus.PENDING);
            assertThat(summary.assetInventoryNumber()).isEqualTo(asset.getInventoryNumber());
            // incident es null en el fixture → hasLinkedIncident debe ser false
            assertThat(summary.hasLinkedIncident()).isFalse();
        }

        @Test
        void should_returnTrueForHasLinkedIncident_when_decommissionHasIncident() {
            AssetDecommission withIncident = buildDecommissionWith(pendingDecommission, incident);
            Pageable pageable = PageRequest.of(0, 10);
            Page<AssetDecommission> page =
                    new PageImpl<>(java.util.List.of(withIncident), pageable, 1);

            when(decommissionRepository.findAllFiltered(null, pageable)).thenReturn(page);

            Page<DecommissionSummaryDTO> result = service.list(null, pageable);

            assertThat(result.getContent().get(0).hasLinkedIncident()).isTrue();
        }

        @Test
        void should_returnEmptyPage_when_noResultsMatch() {
            Pageable pageable = PageRequest.of(0, 10);
            when(decommissionRepository.findAllFiltered(DecommissionStatus.CONFIRMED, pageable))
                    .thenReturn(Page.empty(pageable));

            Page<DecommissionSummaryDTO> result =
                    service.list(DecommissionStatus.CONFIRMED, pageable);

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getByAssetId()
    // =========================================================================

    @Nested
    class GetByAssetId {

        @Test
        void should_returnDecommission_when_assetHasDecommissionProcess() {
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.findByAssetId(asset.getId()))
                    .thenReturn(Optional.of(pendingDecommission));

            DecommissionResponseDTO result = service.getByAssetId(asset.getId());

            assertThat(result.assetId()).isEqualTo(asset.getId());
        }

        @Test
        void should_throwResourceNotFoundException_when_assetNotFound() {
            when(assetRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByAssetId(999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            // El repositorio de bajas nunca se consulta si el bien no existe
            verify(decommissionRepository, never()).findByAssetId(anyLong());
        }

        @Test
        void should_throwResourceNotFoundException_when_assetExistsButHasNoDecommission() {
            // Edge case: el bien existe pero nunca inició proceso de baja
            when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
            when(decommissionRepository.findByAssetId(asset.getId())).thenReturn(Optional.empty());


            Long assetId = asset.getId();

            assertThatThrownBy(() -> service.getByAssetId(assetId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(assetId));
        }
    }

    // =========================================================================
    // Helpers de test
    // =========================================================================

    /**
     * Crea una copia del fixture base con una incidencia asignada.
     * Evita mutar {@code pendingDecommission} entre tests.
     */
    private AssetDecommission buildDecommissionWith(AssetDecommission base, Incident linkedIncident) {
        AssetDecommission copy = new AssetDecommission();
        copy.setId(base.getId());
        copy.setAsset(base.getAsset());
        copy.setIncident(linkedIncident);
        copy.setJustification(base.getJustification());
        copy.setDocumentPath(base.getDocumentPath());
        copy.setDecommissionDate(base.getDecommissionDate());
        copy.setStatus(base.getStatus());
        copy.setCreatedBy(base.getCreatedBy());
        copy.setCreatedAt(base.getCreatedAt());
        return copy;
    }
}