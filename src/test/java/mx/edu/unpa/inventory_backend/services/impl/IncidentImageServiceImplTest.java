package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.*;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentImageResponseDTO;
import mx.edu.unpa.inventory_backend.exceptions.FileStorageException;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.IncidentImageRepository;
import mx.edu.unpa.inventory_backend.repositories.IncidentRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import mx.edu.unpa.inventory_backend.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentImageServiceImplTest {

    // ── Dependencias mockeadas ────────────────────────────────────────────────

    @Mock private IncidentImageRepository imageRepository;
    @Mock private IncidentRepository      incidentRepository;
    @Mock private UserRepository          userRepository;
    @Mock private StorageService          storageService;

    @InjectMocks
    private IncidentImageServiceImpl service;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final Long INCIDENT_ID = 1L;
    private static final Long IMAGE_ID    = 10L;

    private User          operator;
    private Incident      incident;
    private IncidentImage image;

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

        Asset asset = new Asset();
        asset.setId(5L);
        asset.setInventoryNumber("INV-2026-00005");
        asset.setDescription("Laptop Dell");
        asset.setEntryDate(LocalDate.of(2024, 1, 1));
        asset.setLifecycleStatus(LifecycleStatus.ASSIGNED);
        asset.setConditionStatus(ConditionStatus.GOOD);
        asset.setCreatedBy(operator);
        asset.setUpdatedBy(operator);

        incident = new Incident();
        incident.setId(INCIDENT_ID);
        incident.setAsset(asset);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setDescription("Pantalla dañada");
        incident.setIncidentDate(LocalDate.of(2026, 6, 1));
        incident.setConditionAtIncident(ConditionStatus.BAD);
        incident.setCreatedBy(operator);

        image = new IncidentImage();
        image.setId(IMAGE_ID);
        image.setIncident(incident);
        image.setFilePath("incidents/1/images/evidencia.jpg");
        image.setFileName("evidencia.jpg");
        image.setMimeType("image/jpeg");
        image.setUploadedBy(operator);
        image.setUploadedAt(LocalDateTime.of(2026, 6, 1, 10, 0));
    }

    // =========================================================================
    // getByIncidentId()
    // =========================================================================

    @Nested
    class GetByIncidentId {

        @Test
        void should_returnMappedDTOs_when_incidentHasImages() {
            // Arrange
            String publicUrl = "https://server/files/incidents/1/images/evidencia.jpg";
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(imageRepository.findByIncidentIdOrdered(INCIDENT_ID)).thenReturn(List.of(image));
            when(storageService.buildPublicUrl(image.getFilePath())).thenReturn(publicUrl);

            // Act
            List<IncidentImageResponseDTO> result = service.getByIncidentId(INCIDENT_ID);

            // Assert
            assertThat(result).hasSize(1);
            IncidentImageResponseDTO dto = result.get(0);
            assertThat(dto.id()).isEqualTo(IMAGE_ID);
            assertThat(dto.fileName()).isEqualTo("evidencia.jpg");
            assertThat(dto.url()).isEqualTo(publicUrl);
            assertThat(dto.mimeType()).isEqualTo("image/jpeg");
            assertThat(dto.uploadedByName()).isEqualTo(operator.getGuardian().getFullName());
        }

        @Test
        void should_returnEmptyList_when_incidentHasNoImages() {
            // Arrange
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(imageRepository.findByIncidentIdOrdered(INCIDENT_ID)).thenReturn(List.of());

            // Act
            List<IncidentImageResponseDTO> result = service.getByIncidentId(INCIDENT_ID);

            // Assert
            assertThat(result).isEmpty();
            verify(storageService, never()).buildPublicUrl(anyString());
        }

        @Test
        void should_throwResourceNotFoundException_when_incidentNotFound() {
            when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByIncidentId(999L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");

            verify(imageRepository, never()).findByIncidentIdOrdered(anyLong());
        }
    }

    // =========================================================================
    // upload()
    // =========================================================================

    @Nested
    class Upload {

        @Test
        void should_returnImageDTO_when_validJpegIsUploaded() throws IOException {
            // Arrange
            MultipartFile file = validFile("image/jpeg", 512 * 1024L);
            String storedPath = "incidents/1/images/foto.jpg";
            String publicUrl  = "https://server/files/" + storedPath;

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(0L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(storageService.store(eq(file), contains("incidents/" + INCIDENT_ID)))
                    .thenReturn(storedPath);
            // imageRepository.save() retorna el fixture `image` (filePath = evidencia.jpg),
            // por lo que toDTO() llama buildPublicUrl con image.getFilePath(), no con storedPath.
            when(storageService.buildPublicUrl(image.getFilePath())).thenReturn(publicUrl);
            when(imageRepository.save(any(IncidentImage.class))).thenReturn(image);

            // Act
            IncidentImageResponseDTO result = service.upload(INCIDENT_ID, file, operator.getId());

            // Assert
            assertThat(result).isNotNull();
            verify(storageService).store(eq(file), contains("incidents/" + INCIDENT_ID + "/images"));
            verify(imageRepository).save(any(IncidentImage.class));
        }

        @Test
        void should_acceptPng_when_mimeTypeIsPng() {
            MultipartFile file = validFile("image/png", 1024L);

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(0L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(storageService.store(any(), anyString())).thenReturn("path/foto.png");
            when(storageService.buildPublicUrl(anyString())).thenReturn("https://url/foto.png");
            when(imageRepository.save(any())).thenReturn(image);

            assertThatNoException().isThrownBy(
                    () -> service.upload(INCIDENT_ID, file, operator.getId()));
        }

        @Test
        void should_acceptWebp_when_mimeTypeIsWebp() {
            MultipartFile file = validFile("image/webp", 1024L);

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(0L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(storageService.store(any(), anyString())).thenReturn("path/foto.webp");
            when(storageService.buildPublicUrl(anyString())).thenReturn("https://url/foto.webp");
            when(imageRepository.save(any())).thenReturn(image);

            assertThatNoException().isThrownBy(
                    () -> service.upload(INCIDENT_ID, file, operator.getId()));
        }

        // ── Validación de archivo ─────────────────────────────────────────────

        @Test
        void should_throwBadRequest_when_fileIsNull() {
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.upload(INCIDENT_ID, null, operatorId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("vacío");

            // La validación ocurre antes de cualquier consulta a repositorios
            verifyNoInteractions(imageRepository, incidentRepository, storageService);
        }

        @Test
        void should_throwBadRequest_when_fileIsEmpty() {
            MultipartFile emptyFile = mock(MultipartFile.class);
            when(emptyFile.isEmpty()).thenReturn(true);
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.upload(INCIDENT_ID, emptyFile, operatorId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("vacío");

            verifyNoInteractions(imageRepository, incidentRepository, storageService);
        }

        @Test
        void should_throwUnsupportedMediaType_when_mimeTypeIsNotAllowed() {
            // getSize() NO se stubea: validateFile() evalúa tipo antes que tamaño,
            // por lo que si el tipo falla, getSize() nunca se invoca.
            MultipartFile pdf = mock(MultipartFile.class);
            when(pdf.isEmpty()).thenReturn(false);
            when(pdf.getContentType()).thenReturn("application/pdf");
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.upload(INCIDENT_ID, pdf, operatorId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("JPEG, PNG");

            verifyNoInteractions(imageRepository, incidentRepository, storageService);
        }

        @Test
        void should_throwContentTooLarge_when_fileSizeExceeds10Mb() {
            MultipartFile oversized = validFile("image/jpeg", 10 * 1024 * 1024L + 1);
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.upload(INCIDENT_ID, oversized, operatorId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("10 MB");

            verifyNoInteractions(imageRepository, incidentRepository, storageService);
        }

        @Test
        void should_acceptFile_when_sizeIsExactly10Mb() {
            // Boundary: exactamente 10 MB — debe pasar (size > MAX, no >=)
            MultipartFile exactLimit = validFile("image/jpeg", 10 * 1024 * 1024L);

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(0L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(storageService.store(any(), anyString())).thenReturn("path/exact.jpg");
            when(storageService.buildPublicUrl(anyString())).thenReturn("https://url/exact.jpg");
            when(imageRepository.save(any())).thenReturn(image);

            assertThatNoException().isThrownBy(
                    () -> service.upload(INCIDENT_ID, exactLimit, operator.getId()));
        }

        // ── Límite de imágenes ───────────────────────────────────────────────

        @Test
        void should_throwUnprocessableContent_when_maxImagesReached() {
            MultipartFile file = validFile("image/jpeg", 1024L);
            // 8 = MAX_IMAGES definido en la constante del servicio
            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(8L);
            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.upload(INCIDENT_ID, file, operatorId))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("8");

            // El incident nunca se consulta si ya se alcanzó el límite
            verify(incidentRepository, never()).findById(anyLong());
        }

        @Test
        void should_allowUpload_when_imageCountIsOneBelowMax(){
            // Boundary: 7 imágenes actuales — la 8va debe permitirse
            MultipartFile file = validFile("image/jpeg", 1024L);

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(7L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            when(storageService.store(any(), anyString())).thenReturn("path/foto.jpg");
            when(storageService.buildPublicUrl(anyString())).thenReturn("https://url/foto.jpg");
            when(imageRepository.save(any())).thenReturn(image);

            assertThatNoException().isThrownBy(
                    () -> service.upload(INCIDENT_ID, file, operator.getId()));
        }

        // ── Validaciones de dominio ──────────────────────────────────────────

        @Test
        void should_throwResourceNotFoundException_when_incidentNotFound() {
            MultipartFile file = validFile("image/jpeg", 1024L);

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(0L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.empty());

            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.upload(INCIDENT_ID, file, operatorId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(INCIDENT_ID));

            verify(storageService, never()).store(any(), anyString());
        }

        @Test
        void should_throwResourceNotFoundException_when_uploaderNotFound() {
            MultipartFile file = validFile("image/jpeg", 1024L);

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(0L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.upload(INCIDENT_ID, file, 99L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(storageService, never()).store(any(), anyString());
        }

        @Test
        void should_propagateFileStorageException_when_storeThrows() {
            // Si el StorageService falla, la imagen no debe persistirse en BD
            MultipartFile file = validFile("image/jpeg", 1024L);

            when(imageRepository.countByIncidentId(INCIDENT_ID)).thenReturn(0L);
            when(incidentRepository.findById(INCIDENT_ID)).thenReturn(Optional.of(incident));
            when(userRepository.findByIdAndIsActiveTrue(operator.getId()))
                    .thenReturn(Optional.of(operator));
            doThrow(new FileStorageException("Disco lleno"))
                    .when(storageService).store(any(), anyString());

            Long operatorId = operator.getId();
            assertThatThrownBy(() -> service.upload(INCIDENT_ID, file, operatorId))
                    .isInstanceOf(FileStorageException.class);

            verify(imageRepository, never()).save(any());
        }
    }

    // =========================================================================
    // delete()
    // =========================================================================

    @Nested
    class Delete {

        @Test
        void should_deleteImageAndPhysicalFile_when_imageExistsForIncident() throws IOException {
            // El path debe capturarse antes del delete para pasarlo al storage
            String expectedPath = image.getFilePath();

            when(imageRepository.findByIdAndIncidentId(IMAGE_ID, INCIDENT_ID))
                    .thenReturn(Optional.of(image));

            // Act
            service.delete(INCIDENT_ID, IMAGE_ID);

            // Assert — ambas operaciones deben ocurrir en orden: DB primero, disco después
            verify(imageRepository).delete(image);
            verify(storageService).delete(expectedPath);
        }

        @Test
        void should_throwResourceNotFoundException_when_imageNotFoundForIncident() {
            // Edge case: imageId válido pero que pertenece a otra incidencia
            when(imageRepository.findByIdAndIncidentId(IMAGE_ID, INCIDENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(INCIDENT_ID, IMAGE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(IMAGE_ID))
                    .hasMessageContaining(String.valueOf(INCIDENT_ID));

            verify(imageRepository, never()).delete(any());
            verify(storageService, never()).delete(anyString());
        }

        @Test
        void should_throwResourceNotFoundException_when_imageIdDoesNotExist() {
            when(imageRepository.findByIdAndIncidentId(999L, INCIDENT_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(INCIDENT_ID, 999L))
                    .isInstanceOf(ResourceNotFoundException.class);

            verifyNoMoreInteractions(storageService);
        }
    }

    // =========================================================================
    // Helper de test
    // =========================================================================

    /**
     * Crea un MultipartFile mock con los atributos mínimos requeridos por validateFile().
     * isEmpty() retorna false, contentType y size son controlados por el test.
     *
     * getOriginalFilename() NO se stubea aquí porque solo se llega a ejecutar en el service
     * después de que storageService.store() retorna con éxito. Stubear en el helper causaría
     * UnnecessaryStubbingException en todos los tests que abortan antes de ese punto.
     * Como imageRepository.save(any()) retorna el fixture mockeado de todas formas,
     * el valor de fileName no afecta ninguna assertion.
     */
    private MultipartFile validFile(String mimeType, long size) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getContentType()).thenReturn(mimeType);
        when(file.getSize()).thenReturn(size);
        return file;
    }
}