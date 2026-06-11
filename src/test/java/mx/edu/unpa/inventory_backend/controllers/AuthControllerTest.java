package mx.edu.unpa.inventory_backend.controllers;

import mx.edu.unpa.inventory_backend.dtos.auth.request.ChangePasswordRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.request.LoginRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.response.AuthResponse;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.GlobalExceptionHandler;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtAuthenticationFilter;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.AuthService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({GlobalExceptionHandler.class, AuthControllerTest.TestWebConfig.class})
@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController")
class AuthControllerTest {

    private static final String LOGIN_URL           = "/v1/auth/login";
    private static final String CHANGE_PASSWORD_URL = "/v1/auth/change-password";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private RequestPostProcessor principalPostProcessor;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        AuthenticatedUser principal = new AuthenticatedUser(
                1L, "juan.perez", "hashed", UserRole.OPERADOR, true
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

        authResponse = AuthResponse.of(
                "eyJhbGciOiJIUzI1NiJ9.token",
                1L,
                "juan.perez",
                "Juan Pérez",
                UserRole.OPERADOR,
                false
        );
    }

    // =========================================================================
    // POST /v1/auth/login
    // =========================================================================

    @Nested
    @DisplayName("POST /v1/auth/login")
    class Login {

        @Test
        @DisplayName("should_return200WithAuthResponse_when_credentialsAreValid")
        void should_return200WithAuthResponse_when_credentialsAreValid() throws Exception {
            LoginRequest request = new LoginRequest("EMP-001", "password123");
            when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.token").value("eyJhbGciOiJIUzI1NiJ9.token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.userId").value(1))
                    .andExpect(jsonPath("$.data.username").value("juan.perez"))
                    .andExpect(jsonPath("$.data.fullName").value("Juan Pérez"))
                    .andExpect(jsonPath("$.data.role").value("OPERADOR"))
                    .andExpect(jsonPath("$.data.mustChangePassword").value(false));

            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("should_return200WithMustChangePasswordTrue_when_passwordIsGeneric")
        void should_return200WithMustChangePasswordTrue_when_passwordIsGeneric() throws Exception {
            AuthResponse responseWithFlag = AuthResponse.of(
                    "eyJhbGciOiJIUzI1NiJ9.token", 1L,
                    "juan.perez", "Juan Pérez", UserRole.OPERADOR, true
            );
            LoginRequest request = new LoginRequest("EMP-001", "EMP-001");
            when(authService.login(any(LoginRequest.class))).thenReturn(responseWithFlag);

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.mustChangePassword").value(true));
        }

        // ── Validaciones de DTO ────────────────────────────────────────────────

        @Test
        @DisplayName("should_return400WithApiResponse_when_employeeNumberIsBlank")
        void should_return400WithApiResponse_when_employeeNumberIsBlank() throws Exception {
            LoginRequest request = new LoginRequest("", "password123");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("employeeNumber: El número de empleado es obligatorio"));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_passwordIsBlank")
        void should_return400WithApiResponse_when_passwordIsBlank() throws Exception {
            LoginRequest request = new LoginRequest("EMP-001", "");

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("password: La contraseña es obligatoria"));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_requestBodyIsMissing")
        void should_return400WithApiResponse_when_requestBodyIsMissing() throws Exception {
            // HttpMessageNotReadableException → 400 (ver code smell en GlobalExceptionHandler:
            // devuelve ResponseEntity<String> en lugar de ResponseEntity<ApiResponse<Void>>)
            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        // ── Excepciones de dominio ─────────────────────────────────────────────

        @Test
        @DisplayName("should_return401WithApiResponse_when_employeeNumberNotFound")
        void should_return401WithApiResponse_when_employeeNumberNotFound() throws Exception {
            // ResponseStatusException(UNAUTHORIZED) mapeada por el handler de ResponseStatusException
            // agregado al GlobalExceptionHandler. Propaga el status y el reason al ApiResponse.
            LoginRequest request = new LoginRequest("EMP-999", "password123");
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
        }

        @Test
        @DisplayName("should_return401WithApiResponse_when_passwordIsWrong")
        void should_return401WithApiResponse_when_passwordIsWrong() throws Exception {
            // BadCredentialsException extiende AuthenticationException → mapeada a 401
            LoginRequest request = new LoginRequest("EMP-001", "wrongPassword");
            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post(LOGIN_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    // =========================================================================
    // PUT /v1/auth/change-password
    // =========================================================================

    @Nested
    @DisplayName("PUT /v1/auth/change-password")
    class ChangePassword {

        @Test
        @DisplayName("should_return200WithSuccessMessage_when_passwordChangedSuccessfully")
        void should_return200WithSuccessMessage_when_passwordChangedSuccessfully() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass1", "newSecurePass2");
            doNothing().when(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value("Contrasena actualizada correctamente"));

            verify(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));
        }

        @Test
        @DisplayName("should_passAuthenticatedUserIdToService_when_principalIsPresent")
        void should_passAuthenticatedUserIdToService_when_principalIsPresent() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass1", "newSecurePass2");
            doNothing().when(authService).changePassword(eq(1L), any());

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            // Verifica que el userId del principal (1L) llega al servicio
            verify(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));
        }

        // ── Validaciones de DTO ────────────────────────────────────────────────

        @Test
        @DisplayName("should_return400WithApiResponse_when_currentPasswordIsBlank")
        void should_return400WithApiResponse_when_currentPasswordIsBlank() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("", "newSecurePass2");

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_newPasswordIsTooShort")
        void should_return400WithApiResponse_when_newPasswordIsTooShort() throws Exception {
            // @Size(min = 8) en newPassword
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass1", "short");

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message")
                            .value("newPassword: La nueva contraseña debe tener al menos 8 caracteres"));

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_newPasswordIsBlank")
        void should_return400WithApiResponse_when_newPasswordIsBlank() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass1", "");

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").isNotEmpty());

            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_requestBodyIsMissing")
        void should_return400WithApiResponse_when_requestBodyIsMissing() throws Exception {
            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(authService);
        }

        // ── Excepciones de dominio ─────────────────────────────────────────────

        @Test
        @DisplayName("should_return404WithApiResponse_when_userNotFound")
        void should_return404WithApiResponse_when_userNotFound() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass1", "newSecurePass2");
            doThrow(new ResourceNotFoundException("Usuario no encontrado: 1"))
                    .when(authService).changePassword(eq(1L), any());

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Usuario no encontrado: 1"));
        }

        @Test
        @DisplayName("should_return400WithApiResponse_when_currentPasswordIsWrong")
        void should_return400WithApiResponse_when_currentPasswordIsWrong() throws Exception {
            // ResponseStatusException(BAD_REQUEST) mapeada por el handler de ResponseStatusException
            // agregado al GlobalExceptionHandler.
            ChangePasswordRequest request = new ChangePasswordRequest("wrongCurrent", "newSecurePass2");
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta"))
                    .when(authService).changePassword(eq(1L), any());

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta"));
        }

        // Edge case: nueva contraseña igual a la actual — decisión de negocio en el servicio.
        // Verifica que ResponseStatusException se propaga correctamente al cliente.
        @Test
        @DisplayName("should_return400WithApiResponse_when_newPasswordSameAsCurrent")
        void should_return400WithApiResponse_when_newPasswordSameAsCurrent() throws Exception {
            ChangePasswordRequest request = new ChangePasswordRequest("currentPass1", "currentPass1");
            doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede ser igual a la actual"))
                    .when(authService).changePassword(eq(1L), any());

            mockMvc.perform(put(CHANGE_PASSWORD_URL)
                            .with(principalPostProcessor)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("La nueva contraseña no puede ser igual a la actual"));
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