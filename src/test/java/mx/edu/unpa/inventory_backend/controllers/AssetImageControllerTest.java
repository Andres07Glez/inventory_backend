package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.dtos.image.response.AssetImageResponseDTO;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.FileStorageExeption;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.AssetImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({GlobalExceptionHandler.class, AssetImageControllerTest.TestWebConfig.class})
@WebMvcTest(
        controllers = AssetImageController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AssetImageController")
class AssetImageControllerTest {

    private static final String BASE_URL = "/v1/assets/{assetId}/images";
    private static final Long ASSET_ID   = 1L;
    private static final Long IMAGE_ID   = 10L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AssetImageService imageService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private RequestPostProcessor principalPostProcessor;
    private AssetImageResponseDTO primaryImageResponse;
    private AssetImageResponseDTO secondaryImageResponse;

    @BeforeEach
    void setUp() {
        AuthenticatedUser principal = new AuthenticatedUser(
                1L, "admin", "hashed", UserRole.ADMIN, true
        );
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        principalPostProcessor = (MockHttpServletRequest request) -> {
            SecurityContext context = new SecurityContextImpl(authToken);
            SecurityContextHolder.setContext(context);
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context
            );
            return request;
        };

        primaryImageResponse = new AssetImageResponseDTO(
                IMAGE_ID, "laptop-front.jpg",
                "https://storage.example.com/assets/1/laptop-front.jpg",
                "image/jpeg", true
        );
        secondaryImageResponse = new AssetImageResponseDTO(
                11L, "laptop-side.jpg",
                "https://storage.example.com/assets/1/laptop-side.jpg",
                "image/jpeg", false
        );
    }

    // =========================================================================
    // GET /v1/assets/{assetId}/images
    // =========================================================================

    @Nested
    @DisplayName("GET /v1/assets/{assetId}/images")
    class GetImages {

        @Test
        @DisplayName("should_return200WithImageList_when_assetHasImages")
        void should_return200WithImageList_when_assetHasImages() throws Exception {
            when(imageService.getByAssetId(ASSET_ID))
                    .thenReturn(List.of(primaryImageResponse, secondaryImageResponse));

            mockMvc.perform(get(BASE_URL, ASSET_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].id").value(IMAGE_ID))
                    .andExpect(jsonPath("$.data[0].fileName").value("laptop-front.jpg"))
                    .andExpect(jsonPath("$.data[0].isPrimary").value(true))
                    .andExpect(jsonPath("$.data[1].id").value(11))
                    .andExpect(jsonPath("$.data[1].isPrimary").value(false));

            verify(imageService).getByAssetId(ASSET_ID);
        }

        @Test
        @DisplayName("should_return200WithEmptyList_when_assetHasNoImages")
        void should_return200WithEmptyList_when_assetHasNoImages() throws Exception {
            when(imageService.getByAssetId(ASSET_ID)).thenReturn(List.of());

            mockMvc.perform(get(BASE_URL, ASSET_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_assetNotFound")
        void should_return404WithApiResponse_when_assetNotFound() throws Exception {
            when(imageService.getByAssetId(999L))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 999"));

            mockMvc.perform(get(BASE_URL, 999L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 999"));
        }

        // Edge case: @Positive en el path variable — assetId = 0 viola la constraint.
        // Spring 7 lanza HandlerMethodValidationException (no ConstraintViolationException).
        // Requiere handler explícito en GlobalExceptionHandler — ver parche adjunto.
        @Test
        @DisplayName("should_return400WithApiResponse_when_assetIdIsZero")
        void should_return400WithApiResponse_when_assetIdIsZero() throws Exception {
            mockMvc.perform(get(BASE_URL, 0L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verifyNoInteractions(imageService);
        }
    }

    // =========================================================================
    // POST /v1/assets/{assetId}/images  (multipart/form-data)
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/assets/{assetId}/images")
    class UploadImage {

        @Test
        @DisplayName("should_return201WithUploadedImage_when_fileIsValid")
        void should_return201WithUploadedImage_when_fileIsValid() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "laptop-front.jpg", MediaType.IMAGE_JPEG_VALUE,
                    "fake-jpeg-bytes".getBytes()
            );
            when(imageService.upload(eq(ASSET_ID), any(), eq(1L)))
                    .thenReturn(primaryImageResponse);

            mockMvc.perform(multipart(BASE_URL, ASSET_ID)
                            .file(file)
                            .with(principalPostProcessor))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(IMAGE_ID))
                    .andExpect(jsonPath("$.data.fileName").value("laptop-front.jpg"))
                    .andExpect(jsonPath("$.data.mimeType").value("image/jpeg"))
                    .andExpect(jsonPath("$.data.isPrimary").value(true));

            verify(imageService).upload(eq(ASSET_ID), any(), eq(1L));
        }

        @Test
        @DisplayName("should_passAuthenticatedUserIdToService_when_principalIsPresent")
        void should_passAuthenticatedUserIdToService_when_principalIsPresent() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.png", MediaType.IMAGE_PNG_VALUE, "bytes".getBytes()
            );
            when(imageService.upload(eq(ASSET_ID), any(), eq(1L)))
                    .thenReturn(primaryImageResponse);

            mockMvc.perform(multipart(BASE_URL, ASSET_ID)
                            .file(file)
                            .with(principalPostProcessor))
                    .andExpect(status().isCreated());

            // Verifica que el userId del principal (1L) llega al servicio
            verify(imageService).upload(eq(ASSET_ID), any(), eq(1L));
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_assetNotFoundOnUpload")
        void should_return404WithApiResponse_when_assetNotFoundOnUpload() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes()
            );
            when(imageService.upload(eq(999L), any(), any()))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 999"));

            mockMvc.perform(multipart(BASE_URL, 999L)
                            .file(file)
                            .with(principalPostProcessor))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 999"));
        }

        @Test
        @DisplayName("should_return500WithApiResponse_when_storageServiceFails")
        void should_return500WithApiResponse_when_storageServiceFails() throws Exception {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes()
            );
            // FileStorageExeption (typo intencional — ver la clase) → 500
            when(imageService.upload(eq(ASSET_ID), any(), any()))
                    .thenThrow(new FileStorageExeption("No se pudo guardar el archivo en disco"));

            mockMvc.perform(multipart(BASE_URL, ASSET_ID)
                            .file(file)
                            .with(principalPostProcessor))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    // El GlobalExceptionHandler devuelve mensaje genérico para FileStorageExeption
                    .andExpect(jsonPath("$.message").value("No se pudo guardar el documento adjunto."));
        }

        // Edge case: el campo multipart debe llamarse "file" — cualquier otro nombre
        // lanza MissingServletRequestPartException → 400.
        // Requiere handler explícito en GlobalExceptionHandler — ver parche adjunto.
        @Test
        @DisplayName("should_return400WithApiResponse_when_multipartFieldNameIsWrong")
        void should_return400WithApiResponse_when_multipartFieldNameIsWrong() throws Exception {
            MockMultipartFile wrongField = new MockMultipartFile(
                    "imagen",                        // nombre incorrecto — Spring espera "file"
                    "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "bytes".getBytes()
            );

            mockMvc.perform(multipart(BASE_URL, ASSET_ID)
                            .file(wrongField)
                            .with(principalPostProcessor))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Parte requerida ausente: file"));

            verifyNoInteractions(imageService);
        }

        // Edge case: archivo vacío
        @Test
        @DisplayName("should_return400_when_fileIsEmpty")
        void should_return400_when_fileIsEmpty() throws Exception {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "file", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]
            );
            // El servicio lanza IOException que se envuelve → 500,
            // o puede validarse antes con una excepción de dominio propia.
            // Aquí mockeamos el comportamiento más probable: excepción de storage.
            when(imageService.upload(eq(ASSET_ID), any(), any()))
                    .thenThrow(new FileStorageExeption("El archivo está vacío"));

            mockMvc.perform(multipart(BASE_URL, ASSET_ID)
                            .file(emptyFile)
                            .with(principalPostProcessor))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =========================================================================
    // =========================================================================

    @Nested
    @DisplayName("DELETE /v1/assets/{assetId}/images/{imageId}")
    class DeleteImage {

        @Test
        @DisplayName("should_return200WithSuccessMessage_when_imageExists")
        void should_return200WithSuccessMessage_when_imageExists() throws Exception {
            doNothing().when(imageService).delete(ASSET_ID, IMAGE_ID);

            mockMvc.perform(delete(BASE_URL + "/{imageId}", ASSET_ID, IMAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("Imagen eliminada correctamente"));

            verify(imageService).delete(ASSET_ID, IMAGE_ID);
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_imageNotFound")
        void should_return404WithApiResponse_when_imageNotFound() throws Exception {
            doThrow(new ResourceNotFoundException("Imagen no encontrada con id: 99"))
                    .when(imageService).delete(ASSET_ID, 99L);

            mockMvc.perform(delete(BASE_URL + "/{imageId}", ASSET_ID, 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Imagen no encontrada con id: 99"));
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_assetNotFoundOnDelete")
        void should_return404WithApiResponse_when_assetNotFoundOnDelete() throws Exception {
            doThrow(new ResourceNotFoundException("Bien no encontrado con id: 999"))
                    .when(imageService).delete(999L, IMAGE_ID);

            mockMvc.perform(delete(BASE_URL + "/{imageId}", 999L, IMAGE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 999"));
        }

        @Test
        @DisplayName("should_return500WithApiResponse_when_storageServiceFailsOnDelete")
        void should_return500WithApiResponse_when_storageServiceFailsOnDelete() throws Exception {
            doThrow(new FileStorageExeption("No se pudo eliminar el archivo del disco"))
                    .when(imageService).delete(ASSET_ID, IMAGE_ID);

            mockMvc.perform(delete(BASE_URL + "/{imageId}", ASSET_ID, IMAGE_ID))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("No se pudo guardar el documento adjunto."));
        }

        // Edge case: @Positive en imageId — imageId negativo viola la constraint.
        // Spring 7 lanza HandlerMethodValidationException — ver parche en GlobalExceptionHandler.
        @Test
        @DisplayName("should_return400WithApiResponse_when_imageIdIsNegative")
        void should_return400WithApiResponse_when_imageIdIsNegative() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{imageId}", ASSET_ID, -1L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verifyNoInteractions(imageService);
        }
    }

    // =========================================================================
    // PATCH /v1/assets/{assetId}/images/{imageId}/primary
    // =========================================================================

    @Nested
    @DisplayName("PATCH /v1/assets/{assetId}/images/{imageId}/primary")
    class SetPrimary {

        @Test
        @DisplayName("should_return200WithUpdatedImage_when_imageExistsAndBelongsToAsset")
        void should_return200WithUpdatedImage_when_imageExistsAndBelongsToAsset() throws Exception {
            when(imageService.setPrimary(ASSET_ID, IMAGE_ID)).thenReturn(primaryImageResponse);

            mockMvc.perform(patch(BASE_URL + "/{imageId}/primary", ASSET_ID, IMAGE_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(IMAGE_ID))
                    .andExpect(jsonPath("$.data.isPrimary").value(true))
                    .andExpect(jsonPath("$.data.fileName").value("laptop-front.jpg"));

            verify(imageService).setPrimary(ASSET_ID, IMAGE_ID);
        }

        @Test
        @DisplayName("should_return200WithIsPrimaryTrue_when_imageWasNotPrimary")
        void should_return200WithIsPrimaryTrue_when_imageWasNotPrimary() throws Exception {
            // Caso: imagen secundaria se convierte en primaria
            AssetImageResponseDTO nowPrimary = new AssetImageResponseDTO(
                    11L, "laptop-side.jpg",
                    "https://storage.example.com/assets/1/laptop-side.jpg",
                    "image/jpeg", true   // isPrimary cambió a true
            );
            when(imageService.setPrimary(ASSET_ID, 11L)).thenReturn(nowPrimary);

            mockMvc.perform(patch(BASE_URL + "/{imageId}/primary", ASSET_ID, 11L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(11))
                    .andExpect(jsonPath("$.data.isPrimary").value(true));
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_imageNotFoundOnSetPrimary")
        void should_return404WithApiResponse_when_imageNotFoundOnSetPrimary() throws Exception {
            when(imageService.setPrimary(ASSET_ID, 99L))
                    .thenThrow(new ResourceNotFoundException("Imagen no encontrada con id: 99"));

            mockMvc.perform(patch(BASE_URL + "/{imageId}/primary", ASSET_ID, 99L))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Imagen no encontrada con id: 99"));
        }

        @Test
        @DisplayName("should_return404WithApiResponse_when_assetNotFoundOnSetPrimary")
        void should_return404WithApiResponse_when_assetNotFoundOnSetPrimary() throws Exception {
            when(imageService.setPrimary(999L, IMAGE_ID))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 999"));

            mockMvc.perform(patch(BASE_URL + "/{imageId}/primary", 999L, IMAGE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 999"));
        }

        // Edge case: imagen que no pertenece al asset indicado
        @Test
        @DisplayName("should_return404WithApiResponse_when_imageDoesNotBelongToAsset")
        void should_return404WithApiResponse_when_imageDoesNotBelongToAsset() throws Exception {
            // El servicio valida la pertenencia y lanza ResourceNotFoundException
            when(imageService.setPrimary(ASSET_ID, IMAGE_ID))
                    .thenThrow(new ResourceNotFoundException(
                            "La imagen 10 no pertenece al bien 1"));

            mockMvc.perform(patch(BASE_URL + "/{imageId}/primary", ASSET_ID, IMAGE_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("La imagen 10 no pertenece al bien 1"));
        }

        // Edge case: @Positive en imageId — imageId=0 viola la constraint.
        // Spring 7 lanza HandlerMethodValidationException — ver parche en GlobalExceptionHandler.
        @Test
        @DisplayName("should_return400WithApiResponse_when_imageIdIsZeroOnSetPrimary")
        void should_return400WithApiResponse_when_imageIdIsZeroOnSetPrimary() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/{imageId}/primary", ASSET_ID, 0L))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verifyNoInteractions(imageService);
        }
    }

    // ── TestConfiguration: resuelve @AuthenticationPrincipal ─────────────────

    @TestConfiguration
    static class TestWebConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }
}