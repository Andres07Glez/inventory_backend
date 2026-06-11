package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.dtos.asset.request.AssetAssignmentRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.asset.response.AssetAssignmentResponseDTO;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.AssetAssignmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import tools.jackson.databind.ObjectMapper;
// Usa este en lugar de tools.jackson
import java.util.List;


import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({GlobalExceptionHandler.class, AssetAssignmentControllerTest.TestWebConfig.class})@WebMvcTest(
        controllers = AssetAssignmentController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AssetAssignmentController")
class AssetAssignmentControllerTest {

    private static final String BASE_URL = "/v1/assignments";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssetAssignmentService assignmentService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private RequestPostProcessor principalPostProcessor;
    private AssetAssignmentRequestDTO validRequest;
    private AssetAssignmentResponseDTO assignmentResponse;

    @BeforeEach
    void setUp() {
        AuthenticatedUser principal = new AuthenticatedUser(
                1L, "admin", "hashed", UserRole.ADMIN, true
        );
        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        // RequestPostProcessor que coloca el Authentication en el SecurityContextHolder
        // del thread DURANTE la ejecución del request — el único lugar donde
        // AuthenticationPrincipalArgumentResolver lo busca en Spring MVC.
        principalPostProcessor = (MockHttpServletRequest request) -> {
            SecurityContext context = new SecurityContextImpl(authToken);
            SecurityContextHolder.setContext(context);
            // También lo persiste en sesión por compatibilidad con el repositorio de Spring Security
            request.getSession(true).setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context
            );
            return request;
        };

        validRequest = new AssetAssignmentRequestDTO(10L, 5L, "Notas de prueba");

        assignmentResponse = new AssetAssignmentResponseDTO(
                1L,
                "INV-2024-001",
                "Laptop Dell Inspiron",
                "Juan Pérez",
                "Edificio A - Piso 2",
                "Notas de prueba",
                LocalDateTime.of(2024, 6, 10, 9, 0),
                null
        );
    }

    // =========================================================================
    // POST /v1/assignments
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/assignments")
    class CreateAssignment {

        @Test
        @DisplayName("should_return201WithAssignmentData_when_requestIsValid")
        void should_return201WithAssignmentData_when_requestIsValid() throws Exception {
            when(assignmentService.assignAsset(any(AssetAssignmentRequestDTO.class), eq(1L)))
                    .thenReturn(assignmentResponse);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.assetInventoryNumber").value("INV-2024-001"))
                    .andExpect(jsonPath("$.assetDescription").value("Laptop Dell Inspiron"))
                    .andExpect(jsonPath("$.guardianName").value("Juan Pérez"))
                    .andExpect(jsonPath("$.locationName").value("Edificio A - Piso 2"))
                    .andExpect(jsonPath("$.notes").value("Notas de prueba"))
                    .andExpect(jsonPath("$.returnedAt").doesNotExist());

            verify(assignmentService).assignAsset(any(AssetAssignmentRequestDTO.class), eq(1L));
        }

        @Test
        @DisplayName("should_return201WithNullReturnedAt_when_assignmentIsActive")
        void should_return201WithNullReturnedAt_when_assignmentIsActive() throws Exception {
            when(assignmentService.assignAsset(any(), any())).thenReturn(assignmentResponse);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.returnedAt").doesNotExist());
        }

        // ── Validaciones de DTO ────────────────────────────────────────────────

        @Test
        @DisplayName("should_return400_when_assetIdIsNull")
        void should_return400_when_assetIdIsNull() throws Exception {
            AssetAssignmentRequestDTO requestWithNullAsset =
                    new AssetAssignmentRequestDTO(null, 5L, "Notas");

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithNullAsset)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("assetId: El ID del bien es obligatorio"));

            verifyNoInteractions(assignmentService);
        }

        @Test
        @DisplayName("should_return400_when_guardianIdIsNull")
        void should_return400_when_guardianIdIsNull() throws Exception {
            AssetAssignmentRequestDTO requestWithNullGuardian =
                    new AssetAssignmentRequestDTO(10L, null, "Notas");

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestWithNullGuardian)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("guardianId: El ID del resguardante es obligatorio"));

            verifyNoInteractions(assignmentService);
        }

        @Test
        @DisplayName("should_return400_when_requestBodyIsMissing")
        void should_return400_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(assignmentService);
        }

        // ── Excepciones de dominio ─────────────────────────────────────────────

        @Test
        @DisplayName("should_return404_when_assetNotFound")
        void should_return404_when_assetNotFound() throws Exception {
            when(assignmentService.assignAsset(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Bien no encontrado con id: 10"));

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Bien no encontrado con id: 10"));
        }

        @Test
        @DisplayName("should_return404_when_guardianNotFound")
        void should_return404_when_guardianNotFound() throws Exception {
            when(assignmentService.assignAsset(any(), any()))
                    .thenThrow(new ResourceNotFoundException("Resguardante no encontrado con id: 5"));

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Resguardante no encontrado con id: 5"));
        }

        @Test
        @DisplayName("should_return404_when_assigningUserNotFound")
        void should_return404_when_assigningUserNotFound() throws Exception {
            when(assignmentService.assignAsset(any(), eq(1L)))
                    .thenThrow(new ResourceNotFoundException("Usuario asignador no encontrado con id: 1"));

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Usuario asignador no encontrado con id: 1"));
        }

        @Test
        @DisplayName("should_passAuthenticatedUserIdToService_when_principalIsPresent")
        void should_passAuthenticatedUserIdToService_when_principalIsPresent() throws Exception {
            when(assignmentService.assignAsset(any(), eq(1L))).thenReturn(assignmentResponse);

            mockMvc.perform(post(BASE_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validRequest)))
                    .andExpect(status().isCreated());

            verify(assignmentService).assignAsset(any(AssetAssignmentRequestDTO.class), eq(1L));
        }
    }

    @TestConfiguration
    static class TestWebConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            // Le enseña a Spring MVC a resolver @AuthenticationPrincipal
            resolvers.add(new AuthenticationPrincipalArgumentResolver());
        }
    }

}