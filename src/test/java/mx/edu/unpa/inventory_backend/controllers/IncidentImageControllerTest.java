package mx.edu.unpa.inventory_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mx.edu.unpa.inventory_backend.dtos.incident.response.IncidentImageResponseDTO;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.FileStorageException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.IncidentImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {IncidentImageController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(IncidentImageControllerTest.CustomTestConfig.class)
class IncidentImageControllerTest {

    // ── Configuración de prueba ───────────────────────────────────────────────

    @org.springframework.boot.test.context.TestConfiguration
    static class CustomTestConfig implements WebMvcConfigurer {

        @Bean
        @Primary
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(
                        org.springframework.core.MethodParameter parameter) {
                    return parameter.getParameterType().equals(AuthenticatedUser.class);
                }

                @Override
                public Object resolveArgument(
                        org.springframework.core.MethodParameter parameter,
                        org.springframework.web.method.support.ModelAndViewContainer mavContainer,
                        org.springframework.web.context.request.NativeWebRequest webRequest,
                        org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
                    return new AuthenticatedUser(
                            1L, "user@unpa.mx", "pwd_hash", UserRole.OPERADOR, true);
                }
            });
        }
    }

    // ── Dependencias ─────────────────────────────────────────────────────────

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentImageService imageService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ─────────────────────────────────────────────────────────────

    private IncidentImageResponseDTO sampleImage;

    @BeforeEach
    void setUp() {
        sampleImage = new IncidentImageResponseDTO(
                10L,
                "evidencia.jpg",
                "https://storage.example.com/evidencia.jpg",
                "image/jpeg",
                LocalDateTime.of(2026, 6, 11, 10, 0),
                "user@unpa.mx"
        );
    }

    // ── GET /v1/incidents/{incidentId}/images ─────────────────────────────────

    @Test
    void should_returnImageList_when_getImagesCalledWithValidIncidentId() throws Exception {
        when(imageService.getByIncidentId(1L)).thenReturn(List.of(sampleImage));

        mockMvc.perform(get("/v1/incidents/1/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].fileName").value("evidencia.jpg"))
                .andExpect(jsonPath("$.data[0].mimeType").value("image/jpeg"))
                .andExpect(jsonPath("$.data[0].uploadedByName").value("user@unpa.mx"));
    }

    @Test
    void should_returnEmptyList_when_getImagesCalledAndIncidentHasNoImages() throws Exception {
        when(imageService.getByIncidentId(2L)).thenReturn(List.of());

        mockMvc.perform(get("/v1/incidents/2/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void should_return404_when_getImagesCalledWithNonExistentIncidentId() throws Exception {
        doThrow(new ResourceNotFoundException("Incidencia no encontrada con id: 99"))
                .when(imageService).getByIncidentId(99L);

        mockMvc.perform(get("/v1/incidents/99/images"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 99"));
    }

    @Test
    void should_return400_when_getImagesCalledWithNegativeIncidentId() throws Exception {
        mockMvc.perform(get("/v1/incidents/-1/images"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /v1/incidents/{incidentId}/images ────────────────────────────────

    @Test
    void should_return201AndUploadedImage_when_uploadCalledWithValidFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidencia.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes()
        );

        when(imageService.upload(eq(1L), any(), eq(1L))).thenReturn(sampleImage);

        mockMvc.perform(multipart("/v1/incidents/1/images").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.fileName").value("evidencia.jpg"))
                .andExpect(jsonPath("$.data.url")
                        .value("https://storage.example.com/evidencia.jpg"));
    }

    @Test
    void should_return404_when_uploadCalledWithNonExistentIncidentId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidencia.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes()
        );

        doThrow(new ResourceNotFoundException("Incidencia no encontrada con id: 99"))
                .when(imageService).upload(eq(99L), any(), anyLong());

        mockMvc.perform(multipart("/v1/incidents/99/images").file(file))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 99"));
    }

    @Test
    void should_return500_when_uploadFailsDueToStorageError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidencia.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes()
        );

        doThrow(new FileStorageException("No se pudo almacenar el archivo"))
                .when(imageService).upload(eq(1L), any(), anyLong());

        mockMvc.perform(multipart("/v1/incidents/1/images").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("No se pudo guardar el documento adjunto."));
    }

    @Test
    void should_return400_when_uploadCalledWithNegativeIncidentId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidencia.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "fake-image-content".getBytes()
        );

        mockMvc.perform(multipart("/v1/incidents/-1/images").file(file))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_uploadCalledWithMissingFilePart() throws Exception {
        mockMvc.perform(multipart("/v1/incidents/1/images"))
                .andExpect(status().isBadRequest());
    }

    // ── DELETE /v1/incidents/{incidentId}/images/{imageId} ───────────────────

    @Test
    void should_returnSuccessMessage_when_deleteCalledWithValidIds() throws Exception {
        doNothing().when(imageService).delete(1L, 10L);

        mockMvc.perform(delete("/v1/incidents/1/images/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data")
                        .value("Imagen de evidencia eliminada correctamente."));
    }

    @Test
    void should_return404_when_deleteCalledWithNonExistentImageId() throws Exception {
        doThrow(new ResourceNotFoundException("Imagen no encontrada con id: 99"))
                .when(imageService).delete(1L, 99L);

        mockMvc.perform(delete("/v1/incidents/1/images/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Imagen no encontrada con id: 99"));
    }

    @Test
    void should_return404_when_deleteCalledWithNonExistentIncidentId() throws Exception {
        doThrow(new ResourceNotFoundException("Incidencia no encontrada con id: 99"))
                .when(imageService).delete(99L, 10L);

        mockMvc.perform(delete("/v1/incidents/99/images/10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Incidencia no encontrada con id: 99"));
    }

    @Test
    void should_return500_when_deleteFailsDueToStorageError() throws Exception {
        doThrow(new FileStorageException("No se pudo eliminar el archivo del almacenamiento"))
                .when(imageService).delete(1L, 10L);

        mockMvc.perform(delete("/v1/incidents/1/images/10"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("No se pudo guardar el documento adjunto."));
    }

    @Test
    void should_return400_when_deleteCalledWithNegativeIncidentId() throws Exception {
        mockMvc.perform(delete("/v1/incidents/-1/images/10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void should_return400_when_deleteCalledWithNegativeImageId() throws Exception {
        mockMvc.perform(delete("/v1/incidents/1/images/-1"))
                .andExpect(status().isBadRequest());
    }
}