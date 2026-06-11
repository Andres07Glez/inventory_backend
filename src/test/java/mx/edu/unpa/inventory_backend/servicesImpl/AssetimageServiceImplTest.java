package mx.edu.unpa.inventory_backend.servicesImpl;

import mx.edu.unpa.inventory_backend.domains.Asset;
import mx.edu.unpa.inventory_backend.domains.AssetImage;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.image.response.AssetImageResponseDTO;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.AssetImageRepository;
import mx.edu.unpa.inventory_backend.repositories.AssetRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.impl.AssetImageServiceImpl;
import mx.edu.unpa.inventory_backend.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetImageServiceImpl")
class AssetImageServiceImplTest {

    // ─── Mocks ──────────────────────────────────────────────────────────────────
    @Mock private AssetImageRepository imageRepository;
    @Mock private AssetRepository      assetRepository;
    @Mock private UserRepository       userRepository;
    @Mock private StorageService       storageService;

    @InjectMocks
    private AssetImageServiceImpl service;

    // ─── Constantes ─────────────────────────────────────────────────────────────
    private static final Long   ASSET_ID       = 10L;
    private static final Long   IMAGE_ID       = 50L;
    private static final Long   UPLOADER_ID    = 1L;
    private static final String RELATIVE_PATH  = "assets/10/photo.jpg";
    private static final String PUBLIC_URL     = "https://cdn.example.com/" + RELATIVE_PATH;

    // ─── Fixtures base ──────────────────────────────────────────────────────────
    private Asset             asset;
    private User              uploader;
    private MockMultipartFile validFile;

    @BeforeEach
    void setUp() {
        asset = new Asset();
        asset.setId(ASSET_ID);

        uploader = User.builder()
                .id(UPLOADER_ID)
                .username("admin")
                .passwordHash("hash")
                .role(UserRole.ADMIN)
                .isActive(true)
                .build();

        // Happy-path file: todos los stubs son necesarios en el flujo completo
        validFile = buildValidFile("image/jpeg", "photo.jpg");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // getByAssetId
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("getByAssetId")
    class GetByAssetId {

        @Test
        @DisplayName("should_returnEmptyList_when_assetHasNoImages")
        void should_returnEmptyList_when_assetHasNoImages() {
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(imageRepository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(ASSET_ID))
                    .thenReturn(List.of());

            List<AssetImageResponseDTO> result = service.getByAssetId(ASSET_ID);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("should_returnMappedDTOs_when_assetHasImages")
        void should_returnMappedDTOs_when_assetHasImages() {
            AssetImage img1 = buildPersistedImage(51L, "a.jpg", true);
            AssetImage img2 = buildPersistedImage(52L, "b.jpg", false);

            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(imageRepository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(ASSET_ID))
                    .thenReturn(List.of(img1, img2));
            when(storageService.buildPublicUrl(anyString())).thenReturn(PUBLIC_URL);

            List<AssetImageResponseDTO> result = service.getByAssetId(ASSET_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).isPrimary()).isTrue();
            assertThat(result.get(1).isPrimary()).isFalse();
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assetDoesNotExistOnGet")
        void should_throwResourceNotFoundException_when_assetDoesNotExistOnGet() {
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByAssetId(ASSET_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ASSET_ID));
        }

        @Test
        @DisplayName("should_buildPublicUrlForEachImage_when_returningDTOs")
        void should_buildPublicUrlForEachImage_when_returningDTOs() {
            AssetImage img = buildPersistedImage(51L, "a.jpg", true);
            img.setFilePath(RELATIVE_PATH);

            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(imageRepository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(ASSET_ID))
                    .thenReturn(List.of(img));
            when(storageService.buildPublicUrl(RELATIVE_PATH)).thenReturn(PUBLIC_URL);

            List<AssetImageResponseDTO> result = service.getByAssetId(ASSET_ID);

            assertThat(result.get(0).url()).isEqualTo(PUBLIC_URL);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // upload — validaciones de archivo
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("upload — file validation")
    class UploadFileValidation {

        @Test
        @DisplayName("should_throwBadRequest_when_fileIsNull")
        void should_throwBadRequest_when_fileIsNull() {
            assertThatThrownBy(() -> service.upload(ASSET_ID, null, UPLOADER_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @Test
        @DisplayName("should_throwBadRequest_when_fileIsEmpty")
        void should_throwBadRequest_when_fileIsEmpty() {
            MultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

            assertThatThrownBy(() -> service.upload(ASSET_ID, emptyFile, UPLOADER_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.BAD_REQUEST));
        }

        @ParameterizedTest(name = "content-type: {0}")
        @ValueSource(strings = {"application/pdf", "image/gif", "text/plain", "image/bmp"})
        @DisplayName("should_throwUnsupportedMediaType_when_contentTypeIsNotAllowed")
        void should_throwUnsupportedMediaType_when_contentTypeIsNotAllowed(String contentType) {
            MultipartFile badType = new MockMultipartFile("file", "file", contentType, new byte[1024]);

            assertThatThrownBy(() -> service.upload(ASSET_ID, badType, UPLOADER_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE));
        }

        @Test
        @DisplayName("should_throwContentTooLarge_when_fileSizeExceedsTenMB")
        void should_throwContentTooLarge_when_fileSizeExceedsTenMB() {
            long elevenMB = 11 * 1024 * 1024L;
            MultipartFile bigFile = new MockMultipartFile(
                    "file", "big.jpg", "image/jpeg", new byte[(int) elevenMB]);

            assertThatThrownBy(() -> service.upload(ASSET_ID, bigFile, UPLOADER_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.CONTENT_TOO_LARGE));
        }

        @Test
        @DisplayName("should_notThrow_when_fileSizeIsExactlyTenMB")
        void should_notThrow_when_fileSizeIsExactlyTenMB()  {
            // Boundary: exactamente 10 MB debe ser aceptado.
            // El flujo completo se ejecuta → todos los stubs son necesarios.
            long exactlyTenMB = 10 * 1024 * 1024L;
            MultipartFile boundaryFile = buildValidFile("image/png", "max.png", exactlyTenMB);

            stubUploadDependencies(0L, boundaryFile);

            assertThatNoException().isThrownBy(() -> service.upload(ASSET_ID, boundaryFile, UPLOADER_ID));
        }

        @ParameterizedTest(name = "content-type: {0}")
        @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
        @DisplayName("should_acceptFile_when_contentTypeIsAllowed")
        void should_acceptFile_when_contentTypeIsAllowed(String contentType)  {
            MultipartFile allowed = buildValidFile(contentType, "img");
            stubUploadDependencies(0L, allowed);

            assertThatNoException().isThrownBy(() -> service.upload(ASSET_ID, allowed, UPLOADER_ID));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // upload — límite de imágenes y lógica de negocio
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("upload — business logic")
    class UploadBusinessLogic {

        @Test
        @DisplayName("should_throwUnprocessableContent_when_assetAlreadyHasFiveImages")
        void should_throwUnprocessableContent_when_assetAlreadyHasFiveImages() {
            when(imageRepository.countByAssetId(ASSET_ID)).thenReturn(5L);

            assertThatThrownBy(() -> service.upload(ASSET_ID, validFile, UPLOADER_ID))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                            .isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT));
        }

        @Test
        @DisplayName("should_markImageAsPrimary_when_itIsTheFirstImageForTheAsset")
        void should_markImageAsPrimary_when_itIsTheFirstImageForTheAsset() throws IOException {
            stubUploadDependencies(0L, validFile); // count = 0 → primera imagen

            ArgumentCaptor<AssetImage> captor = ArgumentCaptor.forClass(AssetImage.class);
            service.upload(ASSET_ID, validFile, UPLOADER_ID);

            verify(imageRepository).save(captor.capture());
            assertThat(captor.getValue().getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("should_notMarkImageAsPrimary_when_assetAlreadyHasImages")
        void should_notMarkImageAsPrimary_when_assetAlreadyHasImages() throws IOException {
            stubUploadDependencies(3L, validFile); // ya hay 3 imágenes

            ArgumentCaptor<AssetImage> captor = ArgumentCaptor.forClass(AssetImage.class);
            service.upload(ASSET_ID, validFile, UPLOADER_ID);

            verify(imageRepository).save(captor.capture());
            assertThat(captor.getValue().getIsPrimary()).isFalse();
        }

        @Test
        @DisplayName("should_storeFileInCorrectSubDirectory_when_uploading")
        void should_storeFileInCorrectSubDirectory_when_uploading() throws IOException {
            stubUploadDependencies(0L, validFile);

            service.upload(ASSET_ID, validFile, UPLOADER_ID);

            verify(storageService).store(validFile, "assets/" + ASSET_ID);
        }

        @Test
        @DisplayName("should_persistFileNameAndMimeType_when_uploading")
        void should_persistFileNameAndMimeType_when_uploading() throws IOException {
            stubUploadDependencies(0L, validFile);

            ArgumentCaptor<AssetImage> captor = ArgumentCaptor.forClass(AssetImage.class);
            service.upload(ASSET_ID, validFile, UPLOADER_ID);

            verify(imageRepository).save(captor.capture());
            AssetImage saved = captor.getValue();
            assertThat(saved.getFileName()).isEqualTo("photo.jpg");
            assertThat(saved.getMimeType()).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("should_persistRelativePathFromStorage_when_uploading")
        void should_persistRelativePathFromStorage_when_uploading() throws IOException {
            stubUploadDependencies(0L, validFile);

            ArgumentCaptor<AssetImage> captor = ArgumentCaptor.forClass(AssetImage.class);
            service.upload(ASSET_ID, validFile, UPLOADER_ID);

            verify(imageRepository).save(captor.capture());
            assertThat(captor.getValue().getFilePath()).isEqualTo(RELATIVE_PATH);
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assetDoesNotExistOnUpload")
        void should_throwResourceNotFoundException_when_assetDoesNotExistOnUpload() {
            when(imageRepository.countByAssetId(ASSET_ID)).thenReturn(0L);
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.upload(ASSET_ID, validFile, UPLOADER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ASSET_ID));
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_uploaderDoesNotExist")
        void should_throwResourceNotFoundException_when_uploaderDoesNotExist() {
            when(imageRepository.countByAssetId(ASSET_ID)).thenReturn(0L);
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(userRepository.findByIdAndIsActiveTrue(UPLOADER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.upload(ASSET_ID, validFile, UPLOADER_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(UPLOADER_ID));
        }

        @Test
        @DisplayName("should_neverCallStorage_when_fileValidationFails")
        void should_neverCallStorage_when_fileValidationFails() {
            MultipartFile bad = new MockMultipartFile(
                    "file", "doc.pdf", "application/pdf", new byte[1024]);

            assertThatThrownBy(() -> service.upload(ASSET_ID, bad, UPLOADER_ID))
                    .isInstanceOf(ResponseStatusException.class);

            verifyNoInteractions(storageService);
            verifyNoInteractions(assetRepository);
            verifyNoInteractions(imageRepository);
        }

        @Test
        @DisplayName("should_neverCallStorage_when_imageCountLimitIsReached")
        void should_neverCallStorage_when_imageCountLimitIsReached() {
            when(imageRepository.countByAssetId(ASSET_ID)).thenReturn(5L);

            assertThatThrownBy(() -> service.upload(ASSET_ID, validFile, UPLOADER_ID))
                    .isInstanceOf(ResponseStatusException.class);

            verify(storageService, never()).store(any(), anyString());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // delete
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("should_deleteImageAndFile_when_imageExistsAndIsNotPrimary")
        void should_deleteImageAndFile_when_imageExistsAndIsNotPrimary() throws IOException {
            AssetImage image = buildPersistedImage(IMAGE_ID, "x.jpg", false);
            image.setFilePath(RELATIVE_PATH);

            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(image));

            service.delete(ASSET_ID, IMAGE_ID);

            verify(imageRepository).delete(image);
            verify(storageService).delete(RELATIVE_PATH);
        }

        @Test
        @DisplayName("should_promoteOldestRemainingImage_when_deletedImageWasPrimary")
        void should_promoteOldestRemainingImage_when_deletedImageWasPrimary() throws IOException {
            AssetImage primary    = buildPersistedImage(IMAGE_ID, "primary.jpg", true);
            AssetImage nextInLine = buildPersistedImage(52L, "next.jpg", false);
            primary.setFilePath(RELATIVE_PATH);

            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(primary));
            when(imageRepository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(ASSET_ID))
                    .thenReturn(List.of(nextInLine));

            service.delete(ASSET_ID, IMAGE_ID);

            ArgumentCaptor<AssetImage> captor = ArgumentCaptor.forClass(AssetImage.class);
            verify(imageRepository).save(captor.capture());
            assertThat(captor.getValue().getId()).isEqualTo(52L);
            assertThat(captor.getValue().getIsPrimary()).isTrue();
        }

        @Test
        @DisplayName("should_notPromoteAnyImage_when_deletedImageWasPrimaryAndNoImagesRemain")
        void should_notPromoteAnyImage_when_deletedImageWasPrimaryAndNoImagesRemain() throws IOException {
            AssetImage primary = buildPersistedImage(IMAGE_ID, "only.jpg", true);
            primary.setFilePath(RELATIVE_PATH);

            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(primary));
            when(imageRepository.findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(ASSET_ID))
                    .thenReturn(List.of()); // sin imágenes restantes

            service.delete(ASSET_ID, IMAGE_ID);

            // No se debe llamar a save para promover ninguna imagen
            verify(imageRepository, never()).save(any());
            verify(storageService).delete(RELATIVE_PATH);
        }

        @Test
        @DisplayName("should_notCallPromotionQuery_when_deletedImageWasNotPrimary")
        void should_notCallPromotionQuery_when_deletedImageWasNotPrimary() throws IOException {
            AssetImage nonPrimary = buildPersistedImage(IMAGE_ID, "x.jpg", false);
            nonPrimary.setFilePath(RELATIVE_PATH);

            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(nonPrimary));

            service.delete(ASSET_ID, IMAGE_ID);

            // La query de promoción no debe ejecutarse si la imagen no era primaria
            verify(imageRepository, never())
                    .findByAssetIdOrderByIsPrimaryDescUploadedAtAsc(any());
            verify(imageRepository, never()).save(any());
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_imageDoesNotBelongToAsset")
        void should_throwResourceNotFoundException_when_imageDoesNotBelongToAsset() {
            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(ASSET_ID, IMAGE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(IMAGE_ID))
                    .hasMessageContaining(String.valueOf(ASSET_ID));
        }

        @Test
        @DisplayName("should_deletePhysicalFile_even_when_imageWasNotPrimary")
        void should_deletePhysicalFile_even_when_imageWasNotPrimary() throws IOException {
            AssetImage image = buildPersistedImage(IMAGE_ID, "x.jpg", false);
            image.setFilePath(RELATIVE_PATH);

            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(image));

            service.delete(ASSET_ID, IMAGE_ID);

            // storageService.delete es idempotente, debe llamarse siempre
            verify(storageService).delete(RELATIVE_PATH);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // setPrimary
    // ═══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("setPrimary")
    class SetPrimary {

        @Test
        @DisplayName("should_returnDTOWithIsPrimaryTrue_when_imageIsFound")
        void should_returnDTOWithIsPrimaryTrue_when_imageIsFound() {
            AssetImage image = buildPersistedImage(IMAGE_ID, "x.jpg", false);
            AssetImage saved  = buildPersistedImage(IMAGE_ID, "x.jpg", true);

            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(image));
            when(imageRepository.save(image)).thenReturn(saved);
            when(storageService.buildPublicUrl(anyString())).thenReturn(PUBLIC_URL);

            AssetImageResponseDTO result = service.setPrimary(ASSET_ID, IMAGE_ID);

            assertThat(result.isPrimary()).isTrue();
        }

        @Test
        @DisplayName("should_clearAllPrimariesBeforeSetting_when_setPrimaryIsCalled")
        void should_clearAllPrimariesBeforeSetting_when_setPrimaryIsCalled() {
            AssetImage image = buildPersistedImage(IMAGE_ID, "x.jpg", false);

            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(image));
            when(imageRepository.save(image)).thenReturn(image);
            when(storageService.buildPublicUrl(anyString())).thenReturn(PUBLIC_URL);

            service.setPrimary(ASSET_ID, IMAGE_ID);

            // clearPrimary debe ejecutarse ANTES de save
            var inOrder = inOrder(imageRepository);
            inOrder.verify(imageRepository).clearPrimaryByAssetId(ASSET_ID);
            inOrder.verify(imageRepository).save(image);
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_assetDoesNotExistOnSetPrimary")
        void should_throwResourceNotFoundException_when_assetDoesNotExistOnSetPrimary() {
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setPrimary(ASSET_ID, IMAGE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(ASSET_ID));
        }

        @Test
        @DisplayName("should_throwResourceNotFoundException_when_imageDoesNotBelongToAssetOnSetPrimary")
        void should_throwResourceNotFoundException_when_imageDoesNotBelongToAssetOnSetPrimary() {
            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.setPrimary(ASSET_ID, IMAGE_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(IMAGE_ID));
        }

        @Test
        @DisplayName("should_setIsPrimaryTrueOnEntity_when_setPrimaryIsCalled")
        void should_setIsPrimaryTrueOnEntity_when_setPrimaryIsCalled() {
            AssetImage image = buildPersistedImage(IMAGE_ID, "x.jpg", false);

            when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
            when(imageRepository.findByIdAndAssetId(IMAGE_ID, ASSET_ID))
                    .thenReturn(Optional.of(image));
            when(imageRepository.save(image)).thenReturn(image);
            when(storageService.buildPublicUrl(anyString())).thenReturn(PUBLIC_URL);

            service.setPrimary(ASSET_ID, IMAGE_ID);

            ArgumentCaptor<AssetImage> captor = ArgumentCaptor.forClass(AssetImage.class);
            verify(imageRepository).save(captor.capture());
            assertThat(captor.getValue().getIsPrimary()).isTrue();
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Stub común para el flujo feliz de upload.
     * Recibe el archivo explícitamente para que Mockito pueda rastrear
     * qué stubs del file mock se consumen realmente en cada test.
     */
    private void stubUploadDependencies(long existingCount, MultipartFile file) {
        when(imageRepository.countByAssetId(ASSET_ID)).thenReturn(existingCount);
        when(assetRepository.findById(ASSET_ID)).thenReturn(Optional.of(asset));
        when(userRepository.findByIdAndIsActiveTrue(UPLOADER_ID)).thenReturn(Optional.of(uploader));
        when(storageService.store(eq(file), anyString())).thenReturn(RELATIVE_PATH);
        when(storageService.buildPublicUrl(RELATIVE_PATH)).thenReturn(PUBLIC_URL);

        AssetImage saved = buildPersistedImage(IMAGE_ID, "photo.jpg", existingCount == 0);
        saved.setFilePath(RELATIVE_PATH);
        when(imageRepository.save(any(AssetImage.class))).thenReturn(saved);
    }

    /**
     * Instancia real de MockMultipartFile que pasa todas las validaciones.
     * No es un mock de Mockito → nunca genera UnnecessaryStubbingException.
     */
    private MockMultipartFile buildValidFile(String contentType, String originalName) {
        return buildValidFile(contentType, originalName, 1024L);
    }

    /** Variante con tamaño configurable; útil para tests de boundary (ej. exactamente 10 MB). */
    private MockMultipartFile buildValidFile(String contentType, String originalName, long size) {
        return new MockMultipartFile("file", originalName, contentType, new byte[(int) size]);
    }

    /** Crea una entidad AssetImage simulando que ya fue persistida (tiene id). */
    private AssetImage buildPersistedImage(Long id, String fileName, boolean isPrimary) {
        AssetImage img = new AssetImage();
        img.setId(id);
        img.setAsset(asset);
        img.setFileName(fileName);
        img.setFilePath("assets/" + ASSET_ID + "/" + fileName);
        img.setMimeType("image/jpeg");
        img.setIsPrimary(isPrimary);
        img.setUploadedBy(uploader);
        return img;
    }
}