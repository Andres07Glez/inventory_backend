package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.auth.request.ChangePasswordRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.request.LoginRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.response.AuthResponse;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private AuthenticationManager authManager;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers — siempre con builder para respetar @Builder.Default de Lombok
    // ─────────────────────────────────────────────────────────────────────────

    private Guardian buildGuardian(String employeeNumber, String fullName) {
        Guardian guardian = new Guardian();
        guardian.setId(1L);
        guardian.setEmployeeNumber(employeeNumber);
        guardian.setFullName(fullName);
        guardian.setIsActive(true);
        return guardian;
    }

    private User buildActiveUser(Long id, String username, String passwordHash,
                                 Guardian guardian, UserRole role) {
        return User.builder()
                .id(id)
                .username(username)
                .passwordHash(passwordHash)
                .guardian(guardian)
                .role(role)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // login()
    // =========================================================================
    @Nested
    class Login {

        private static final String EMPLOYEE_NUMBER = "EMP-001";
        private static final String PLAIN_PASSWORD   = "password123";
        private static final String HASHED_PASSWORD  = "$2a$10$hashedValue";
        private static final String GENERATED_TOKEN  = "eyJhbGciOiJIUzI1NiJ9.token";

        private LoginRequest loginRequest;
        private Guardian     guardian;
        private User         user;

        @BeforeEach
        void setUp() {
            loginRequest = new LoginRequest(EMPLOYEE_NUMBER, PLAIN_PASSWORD);
            guardian     = buildGuardian(EMPLOYEE_NUMBER, "Juan Pérez");
            user         = buildActiveUser(1L, "juan.perez", HASHED_PASSWORD, guardian, UserRole.OPERADOR);
        }

        @Test
        void should_returnAuthResponse_when_credentialsAreValid() {
            // Arrange
            when(userRepository.findByGuardianEmployeeNumber(EMPLOYEE_NUMBER))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(EMPLOYEE_NUMBER, HASHED_PASSWORD))
                    .thenReturn(false); // contraseña NO es la genérica → mustChangePassword=false
            when(jwtService.generateToken(any(AuthenticatedUser.class)))
                    .thenReturn(GENERATED_TOKEN);

            // Act
            AuthResponse response = authService.login(loginRequest);

            // Assert
            assertNotNull(response);
            assertEquals(GENERATED_TOKEN,       response.token());
            assertEquals("Bearer",              response.tokenType());
            assertEquals(1L,                    response.userId());
            assertEquals("juan.perez",          response.username());
            assertEquals("Juan Pérez",          response.fullName());
            assertEquals(UserRole.OPERADOR,     response.role());
            assertFalse(response.mustChangePassword());
        }

        @Test
        void should_setMustChangePasswordTrue_when_passwordMatchesEmployeeNumber() {
            // Arrange — la contraseña almacenada es el número de empleado (caso inicial)
            when(userRepository.findByGuardianEmployeeNumber(EMPLOYEE_NUMBER))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(EMPLOYEE_NUMBER, HASHED_PASSWORD))
                    .thenReturn(true); // contraseña genérica sin cambiar
            when(jwtService.generateToken(any(AuthenticatedUser.class)))
                    .thenReturn(GENERATED_TOKEN);

            // Act
            AuthResponse response = authService.login(loginRequest);

            // Assert
            assertTrue(response.mustChangePassword());
        }

        @Test
        void should_updateLastLoginAt_when_loginSucceeds() {
            // Arrange
            when(userRepository.findByGuardianEmployeeNumber(EMPLOYEE_NUMBER))
                    .thenReturn(Optional.of(user));
            when(jwtService.generateToken(any(AuthenticatedUser.class)))
                    .thenReturn(GENERATED_TOKEN);

            // Act
            authService.login(loginRequest);

            // Assert — capturamos el User que se le pasó a save()
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertNotNull(userCaptor.getValue().getLastLoginAt());
        }

        @Test
        void should_throwResponseStatusException_when_employeeNumberNotFound() {
            // Arrange
            when(userRepository.findByGuardianEmployeeNumber(EMPLOYEE_NUMBER))
                    .thenReturn(Optional.empty());

            // Act & Assert
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.login(loginRequest)
            );
            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
            // No verificamos el mensaje exacto para evitar acoplar el test a strings de UI
        }

        @Test
        void should_propagateBadCredentialsException_when_authManagerRejects() {
            // Arrange — el usuario existe pero la contraseña es incorrecta
            when(userRepository.findByGuardianEmployeeNumber(EMPLOYEE_NUMBER))
                    .thenReturn(Optional.of(user));
            doThrow(new BadCredentialsException("Bad credentials"))
                    .when(authManager)
                    .authenticate(any(UsernamePasswordAuthenticationToken.class));

            // Act & Assert
            assertThrows(
                    BadCredentialsException.class,
                    () -> authService.login(loginRequest)
            );
            // El repo no debe guardarse si la autenticación falla
            verify(userRepository, never()).save(any());
        }

        @Test
        void should_authenticateWithRealUsername_not_employeeNumber() {
            // Rationale: Spring Security usa username, no employeeNumber internamente
            when(userRepository.findByGuardianEmployeeNumber(EMPLOYEE_NUMBER))
                    .thenReturn(Optional.of(user));
            when(jwtService.generateToken(any(AuthenticatedUser.class)))
                    .thenReturn(GENERATED_TOKEN);

            // Act
            authService.login(loginRequest);

            // Assert — el token de autenticación usa el username real del User
            ArgumentCaptor<UsernamePasswordAuthenticationToken> tokenCaptor =
                    ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
            verify(authManager).authenticate(tokenCaptor.capture());
            assertEquals("juan.perez", tokenCaptor.getValue().getPrincipal());
            assertEquals(PLAIN_PASSWORD, tokenCaptor.getValue().getCredentials());
        }

        @Test
        void should_buildAuthenticatedUserWithCorrectRole_when_userIsAdmin() {
            // Edge case: el rol ADMIN debe propagarse al token JWT
            User adminUser = buildActiveUser(2L, "admin.user", HASHED_PASSWORD,
                    buildGuardian(EMPLOYEE_NUMBER, "Admin User"), UserRole.ADMIN);

            when(userRepository.findByGuardianEmployeeNumber(EMPLOYEE_NUMBER))
                    .thenReturn(Optional.of(adminUser));
            when(jwtService.generateToken(any(AuthenticatedUser.class)))
                    .thenReturn(GENERATED_TOKEN);

            ArgumentCaptor<AuthenticatedUser> principalCaptor =
                    ArgumentCaptor.forClass(AuthenticatedUser.class);

            // Act
            AuthResponse response = authService.login(loginRequest);

            // Assert
            verify(jwtService).generateToken(principalCaptor.capture());
            assertEquals(UserRole.ADMIN, principalCaptor.getValue().role());
            assertEquals(UserRole.ADMIN, response.role());
        }
    }

    // =========================================================================
    // changePassword()
    // =========================================================================
    @Nested
    class ChangePassword {

        private static final Long   USER_ID          = 1L;
        private static final String CURRENT_PASSWORD = "currentPass1";
        private static final String NEW_PASSWORD     = "newSecurePass2";
        private static final String CURRENT_HASH     = "$2a$10$currentHash";
        private static final String NEW_HASH         = "$2a$10$newHash";

        private User                  user;
        private ChangePasswordRequest request;

        @BeforeEach
        void setUp() {
            Guardian guardian = buildGuardian("EMP-001", "Juan Pérez");
            user    = buildActiveUser(USER_ID, "juan.perez", CURRENT_HASH, guardian, UserRole.OPERADOR);
            request = new ChangePasswordRequest(CURRENT_PASSWORD, NEW_PASSWORD);
        }

        @Test
        void should_updatePasswordHash_when_currentPasswordIsCorrect() {
            // Arrange
            when(userRepository.findByIdAndIsActiveTrue(USER_ID))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH))
                    .thenReturn(true);
            when(passwordEncoder.encode(NEW_PASSWORD))
                    .thenReturn(NEW_HASH);

            // Act
            authService.changePassword(USER_ID, request);

            // Assert
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals(NEW_HASH, userCaptor.getValue().getPasswordHash());
        }

        @Test
        void should_throwBadRequest_when_currentPasswordDoesNotMatch() {
            // Arrange
            when(userRepository.findByIdAndIsActiveTrue(USER_ID))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH))
                    .thenReturn(false);

            // Act & Assert
            ResponseStatusException ex = assertThrows(
                    ResponseStatusException.class,
                    () -> authService.changePassword(USER_ID, request)
            );
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            verify(userRepository, never()).save(any());
        }

        @Test
        void should_throwResourceNotFoundException_when_userNotFoundOrInactive() {
            // Arrange — usuario no existe o está inactivo (findByIdAndIsActiveTrue filtra ambos)
            when(userRepository.findByIdAndIsActiveTrue(USER_ID))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(
                    ResourceNotFoundException.class,
                    () -> authService.changePassword(USER_ID, request)
            );
            verify(passwordEncoder, never()).matches(anyString(), anyString());
            verify(userRepository,  never()).save(any());
        }

        @Test
        void should_encodeNewPassword_before_saving() {
            // Edge case: asegura que nunca se guarda la contraseña en texto plano
            when(userRepository.findByIdAndIsActiveTrue(USER_ID))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH))
                    .thenReturn(true);
            when(passwordEncoder.encode(NEW_PASSWORD))
                    .thenReturn(NEW_HASH);

            // Act
            authService.changePassword(USER_ID, request);

            // Assert — encode fue llamado con la nueva contraseña en texto plano
            verify(passwordEncoder).encode(NEW_PASSWORD);
            // Y el hash resultante fue el que se persistió
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertNotEquals(NEW_PASSWORD, captor.getValue().getPasswordHash(),
                    "La contraseña NO debe guardarse en texto plano");
            assertEquals(NEW_HASH, captor.getValue().getPasswordHash());
        }

        @Test
        void should_notChangeOtherUserFields_when_passwordIsUpdated() {
            // Edge case: el cambio de contraseña no debe alterar otros campos del usuario
            when(userRepository.findByIdAndIsActiveTrue(USER_ID))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(CURRENT_PASSWORD, CURRENT_HASH))
                    .thenReturn(true);
            when(passwordEncoder.encode(NEW_PASSWORD))
                    .thenReturn(NEW_HASH);

            // Act
            authService.changePassword(USER_ID, request);

            // Assert
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertEquals("juan.perez",      saved.getUsername());
            assertEquals(UserRole.OPERADOR, saved.getRole());
            assertTrue(saved.getIsActive());
        }
    }
}