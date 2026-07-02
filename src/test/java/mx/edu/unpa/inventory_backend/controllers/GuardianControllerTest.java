package mx.edu.unpa.inventory_backend.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import mx.edu.unpa.inventory_backend.dtos.guardian.request.GuardianRequestDTO;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianResponseDTO;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.DuplicateResourceException;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.GuardianService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = {GuardianController.class, GlobalExceptionHandler.class},
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@Import(GuardianControllerTest.CustomTestConfig.class)
class GuardianControllerTest {

    @Autowired MockMvc        mockMvc;
    @Autowired ObjectMapper   objectMapper;
    @MockitoBean GuardianService guardianService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    private GuardianResponseDTO sampleResponse;
    private GuardianRequestDTO  validRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new GuardianResponseDTO(
                1L, "EMP-001", "Juan Pérez", "juan@unpa.mx",
                "555-1234", "Sistemas", 1, "Edificio A", true
        );
        validRequest = new GuardianRequestDTO(
                "EMP-001", "Juan Pérez", "juan@unpa.mx",
                "555-1234", "Sistemas", 1
        );
    }

    // =========================================================
    // GET /v1/guardians  — devuelve Page<> directamente (sin ApiResponse)
    // =========================================================

    @Test
    void should_returnPageOfGuardians_when_findAllActiveIsCalled() throws Exception {
        Page<GuardianResponseDTO> page = new PageImpl<>(List.of(sampleResponse));
        when(guardianService.findAllActive(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/guardians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].fullName").value("Juan Pérez"))
                .andExpect(jsonPath("$.content[0].employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_returnEmptyPage_when_noActiveGuardiansExist() throws Exception {
        when(guardianService.findAllActive(any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/v1/guardians"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.content").isEmpty());
    }

    // =========================================================
    // GET /v1/guardians/search  — devuelve Page<> directamente (sin ApiResponse)
    // =========================================================

    @Test
    void should_returnMatchingGuardians_when_searchTermIsValid() throws Exception {
        Page<GuardianResponseDTO> page = new PageImpl<>(List.of(sampleResponse));
        when(guardianService.search(eq("Juan"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/v1/guardians/search").param("q", "Juan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Juan Pérez"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void should_returnEmptyPage_when_searchTermMatchesNoGuardians() throws Exception {
        when(guardianService.search(eq("XYZ"), any(Pageable.class))).thenReturn(Page.empty());

        mockMvc.perform(get("/v1/guardians/search").param("q", "XYZ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void should_return400_when_searchTermIsBlank() throws Exception {
        // @NotBlank sobre @RequestParam con @Validated → ConstraintViolationException → 400
        mockMvc.perform(get("/v1/guardians/search").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_searchTermExceedsMaxLength() throws Exception {
        // @Size(max = 100) → ConstraintViolationException → 400
        String tooLong = "A".repeat(101);

        mockMvc.perform(get("/v1/guardians/search").param("q", tooLong))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return400_when_searchParamIsMissing() throws Exception {
        // Sin ?q= → MissingServletRequestParameterException → 400
        mockMvc.perform(get("/v1/guardians/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("q")));
    }

    // =========================================================
    // GET /v1/guardians/id
    // =========================================================

    @Test
    void should_returnGuardian_when_idExists() throws Exception {
        when(guardianService.findById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/v1/guardians/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.fullName").value("Juan Pérez"))
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.data.email").value("juan@unpa.mx"))
                .andExpect(jsonPath("$.data.isActive").value(true));
    }

    @Test
    void should_return404_when_guardianDoesNotExist() throws Exception {
        doThrow(new ResourceNotFoundException("Resguardante no encontrado con id: 99"))
                .when(guardianService).findById(99L);

        mockMvc.perform(get("/v1/guardians/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Resguardante no encontrado con id: 99"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // =========================================================
    // POST /v1/guardians
    // =========================================================

    @Test
    void should_return201_when_guardianIsCreatedSuccessfully() throws Exception {
        when(guardianService.create(any(GuardianRequestDTO.class))).thenReturn(sampleResponse);

        mockMvc.perform(post("/v1/guardians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.employeeNumber").value("EMP-001"))
                .andExpect(jsonPath("$.data.fullName").value("Juan Pérez"));
    }

    @Test
    void should_return400_when_employeeNumberIsMissing() throws Exception {
        GuardianRequestDTO noEmployeeNumber = new GuardianRequestDTO(
                null, "Juan Pérez", "juan@unpa.mx", "555-1234", "Sistemas", 1
        );

        mockMvc.perform(post("/v1/guardians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noEmployeeNumber)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("employeeNumber")));
    }

    @Test
    void should_return400_when_fullNameIsMissing() throws Exception {
        GuardianRequestDTO noFullName = new GuardianRequestDTO(
                "EMP-001", null, "juan@unpa.mx", "555-1234", "Sistemas", 1
        );

        mockMvc.perform(post("/v1/guardians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noFullName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("fullName")));
    }

    @Test
    void should_return400_when_emailFormatIsInvalid() throws Exception {
        GuardianRequestDTO badEmail = new GuardianRequestDTO(
                "EMP-001", "Juan Pérez", "no-es-un-email", "555-1234", "Sistemas", 1
        );

        mockMvc.perform(post("/v1/guardians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("email")));
    }

    @Test
    void should_return409_when_employeeNumberAlreadyExists() throws Exception {
        doThrow(new DuplicateResourceException("El número de empleado ya está registrado: EMP-001"))
                .when(guardianService).create(any(GuardianRequestDTO.class));

        mockMvc.perform(post("/v1/guardians")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El número de empleado ya está registrado: EMP-001"));
    }

    // =========================================================
    // PUT /v1/guardians/id
    // =========================================================

    @Test
    void should_return200_when_guardianIsUpdatedSuccessfully() throws Exception {
        GuardianResponseDTO updated = new GuardianResponseDTO(
                1L, "EMP-001", "Juan Pérez Modificado", "juan.nuevo@unpa.mx",
                "555-9999", "TI", 2, "Edificio B", true
        );
        when(guardianService.update(eq(1L), any(GuardianRequestDTO.class))).thenReturn(updated);

        mockMvc.perform(put("/v1/guardians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Juan Pérez Modificado"))
                .andExpect(jsonPath("$.data.department").value("TI"));
    }

    @Test
    void should_return400_when_updateRequestHasValidationErrors() throws Exception {
        GuardianRequestDTO blankFields = new GuardianRequestDTO(
                "", "", null, null, null, null
        );

        mockMvc.perform(put("/v1/guardians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(blankFields)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_return404_when_updatingNonExistentGuardian() throws Exception {
        doThrow(new ResourceNotFoundException("Resguardante no encontrado con id: 99"))
                .when(guardianService).update(eq(99L), any(GuardianRequestDTO.class));

        mockMvc.perform(put("/v1/guardians/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void should_return409_when_updatingWithDuplicateEmployeeNumber() throws Exception {
        doThrow(new DuplicateResourceException("El número de empleado ya está registrado: EMP-001"))
                .when(guardianService).update(eq(1L), any(GuardianRequestDTO.class));

        mockMvc.perform(put("/v1/guardians/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("El número de empleado ya está registrado: EMP-001"));
    }

    // =========================================================
    // DELETE /v1/guardians/id
    // =========================================================

    @Test
    void should_return204_when_guardianIsDeactivatedSuccessfully() throws Exception {
        // guardianService.deactivate() es void → no necesita when(), solo verificar el status
        mockMvc.perform(delete("/v1/guardians/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return404_when_deactivatingNonExistentGuardian() throws Exception {
        doThrow(new ResourceNotFoundException("Resguardante no encontrado con id: 99"))
                .when(guardianService).deactivate(99L);

        mockMvc.perform(delete("/v1/guardians/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Resguardante no encontrado con id: 99"));
    }

    // =========================================================
    // TestConfiguration
    // =========================================================

    @TestConfiguration
    static class CustomTestConfig implements WebMvcConfigurer {

        /**
         * ObjectMapper primario con soporte para LocalDate/LocalDateTime.
         * Sin esto, Jackson serializa fechas como arrays numéricos → tests fallan.
         */
        @Bean
        @Primary
        ObjectMapper objectMapper() {
            return new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }

        /**
         * Resuelve @AuthenticationPrincipal AuthenticatedUser en métodos del controlador.
         * Necesario porque la seguridad está desactivada y Spring no inyecta el principal
         * del SecurityContext. GuardianController no lo usa actualmente, pero se registra
         * por consistencia con el lineamiento del proyecto y para soportar futuros cambios.
         */
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.getParameterType().equals(AuthenticatedUser.class);
                }

                @Override
                public Object resolveArgument(
                        MethodParameter parameter,
                        ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest,
                        WebDataBinderFactory binderFactory
                ) {
                    return new AuthenticatedUser(1L, "user@unpa.mx", "pwd_hash", UserRole.OPERADOR, true,1L);
                }
            });
        }
    }
}