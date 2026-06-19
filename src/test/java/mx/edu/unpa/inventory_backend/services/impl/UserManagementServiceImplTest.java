package mx.edu.unpa.inventory_backend.services.impl;

import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.guardian.response.GuardianSummary;
import mx.edu.unpa.inventory_backend.dtos.user.request.CreateUserRequest;
import mx.edu.unpa.inventory_backend.dtos.user.request.UpdateUserRoleRequest;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserSummaryResponse;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.UserMapper;
import mx.edu.unpa.inventory_backend.repositories.GuardianRepository;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock private UserRepository      userRepository;
    @Mock private GuardianRepository  guardianRepository;
    // UserMapper usa componentModel=SPRING → bean gestionado por Spring.
    // Se mockea para no necesitar contexto ni la implementación generada por MapStruct.
    @Mock private UserMapper          userMapper;
    // PasswordEncoder es interfaz de Spring Security → se mockea para controlar
    // el hash devuelto sin depender de BCrypt en tests unitarios.
    @Mock private PasswordEncoder     passwordEncoder;

    @InjectMocks
    private UserManagementServiceImpl userManagementService;

    // ─────────────────────────────────────────────
    //  Factories de stubs
    // ─────────────────────────────────────────────

    /**
     * Guardian stub con employeeNumber.
     * resetPassword() y create() navegan guardian.getEmployeeNumber()
     * para generar la contraseña inicial → el campo es obligatorio en los stubs.
     */
    private Guardian stubGuardian(Long id, String employeeNumber, String fullName) {
        Guardian g = new Guardian();
        g.setId(id);
        g.setEmployeeNumber(employeeNumber);
        g.setFullName(fullName);
        g.setIsActive(true);
        return g;
    }

    /**
     * User stub activo con Guardian anidado.
     * Usar siempre User.builder() — la entidad tiene @Builder con @Builder.Default
     * en role e isActive; new User() no activaría esos defaults (Problema 2 del contexto).
     */
    private User stubUser(Long id, String username, UserRole role, Guardian guardian) {
        User u = User.builder()
                .username(username)
                .passwordHash("$2a$10$hashed")
                .role(role)
                .isActive(true)
                .build();
        u.setId(id);
        u.setGuardian(guardian);
        return u;
    }

    private User stubUser(Long id, String username, UserRole role) {
        return stubUser(id, username, role, stubGuardian(10L, "EMP-001", "Guardian Test"));
    }

    /** UserDetailResponse stub mínimo para stubear el mapper. */
    private UserDetailResponse stubDetailResponse(Long id, String username, UserRole role, boolean isActive) {
        GuardianSummary gs = new GuardianSummary(10L, "Guardian Test", "EMP-001", null);
        return new UserDetailResponse(
                id, username, "Guardian Test", "test@unpa.edu.mx",
                "EMP-001", role, isActive,
                null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                gs
        );
    }

    private UserSummaryResponse stubSummaryResponse(Long id, String username, UserRole role) {
        GuardianSummary gs = new GuardianSummary(10L, "Guardian Test", "EMP-001", null);
        return new UserSummaryResponse(
                id, username, "Guardian Test", "test@unpa.edu.mx",
                "EMP-001", role, true,
                null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 1, 1, 0, 0),
                gs
        );
    }

    // ─────────────────────────────────────────────
    //  findAll
    // ─────────────────────────────────────────────

    @Test
    void should_returnPageOfSummaries_when_findAllWithFilters() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        User u = stubUser(1L, "usr_admin", UserRole.ADMIN);
        Page<User> userPage = new PageImpl<>(List.of(u), pageable, 1);
        UserSummaryResponse summary = stubSummaryResponse(1L, "usr_admin", UserRole.ADMIN);

        when(userRepository.findWithFilters("admin", UserRole.ADMIN, true, pageable))
                .thenReturn(userPage);
        when(userMapper.toSummary(u)).thenReturn(summary);

        // Act
        Page<UserSummaryResponse> result = userManagementService
                .findAll("admin", UserRole.ADMIN, true, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("usr_admin", result.getContent().get(0).username());
        verify(userRepository).findWithFilters("admin", UserRole.ADMIN, true, pageable);
    }

    @Test
    void should_passNullSearchTerm_when_findAllWithBlankSearch() {
        // Arrange — search en blanco debe normalizarse a null antes de llamar al repo
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findWithFilters(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        userManagementService.findAll("   ", null, null, pageable);

        // Assert — el repo recibe null, no el string en blanco
        verify(userRepository).findWithFilters(null, null, null, pageable);
    }

    @Test
    void should_passNullSearchTerm_when_findAllWithNullSearch() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findWithFilters(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        userManagementService.findAll(null, null, null, pageable);

        // Assert
        verify(userRepository).findWithFilters(null, null, null, pageable);
    }

    @Test
    void should_returnEmptyPage_when_findAllAndNoUsersMatch() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findWithFilters(any(), any(), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<UserSummaryResponse> result = userManagementService
                .findAll("xyz_inexistente", null, null, pageable);

        // Assert
        assertTrue(result.isEmpty());
    }

    // ─────────────────────────────────────────────
    //  findById
    // ─────────────────────────────────────────────

    @Test
    void should_returnDetailResponse_when_findByIdAndUserExists() {
        // Arrange
        User user = stubUser(1L, "usr_operador", UserRole.OPERADOR);
        UserDetailResponse expected = stubDetailResponse(1L, "usr_operador", UserRole.OPERADOR, true);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDetail(user)).thenReturn(expected);

        // Act
        UserDetailResponse result = userManagementService.findById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L,              result.id());
        assertEquals("usr_operador",  result.username());
        assertEquals(UserRole.OPERADOR, result.role());
    }

    @Test
    void should_throwResourceNotFoundException_when_findByIdAndUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userManagementService.findById(99L)
        );
        assertTrue(ex.getMessage().contains("99"));
    }

    // ─────────────────────────────────────────────
    //  create — happy path
    // ─────────────────────────────────────────────

    @Test
    void should_returnDetailResponse_when_createWithValidRequest() {
        // Arrange
        Guardian guardian = stubGuardian(10L, "EMP-100", "Ana Lopez");
        CreateUserRequest request = new CreateUserRequest("usr_alopez", UserRole.OPERADOR, 10L);
        User savedUser = stubUser(5L, "usr_alopez", UserRole.OPERADOR, guardian);
        UserDetailResponse expected = stubDetailResponse(5L, "usr_alopez", UserRole.OPERADOR, true);

        when(guardianRepository.findById(10L)).thenReturn(Optional.of(guardian));
        when(userRepository.existsByGuardianId(10L)).thenReturn(false);
        when(userRepository.existsByUsername("usr_alopez")).thenReturn(false);
        when(passwordEncoder.encode("EMP-100")).thenReturn("$2a$10$hashed_emp100");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDetail(savedUser)).thenReturn(expected);

        // Act
        UserDetailResponse result = userManagementService.create(request);

        // Assert
        assertNotNull(result);
        assertEquals(5L,            result.id());
        assertEquals("usr_alopez",  result.username());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void should_encodeGuardianEmployeeNumberAsInitialPassword_when_create() {
        // Arrange — la contraseña inicial es el número de empleado del guardian codificado con BCrypt
        Guardian guardian = stubGuardian(10L, "EMP-200", "Pedro Rios");
        CreateUserRequest request = new CreateUserRequest("usr_prios", UserRole.AUDITOR, 10L);
        User savedUser = stubUser(6L, "usr_prios", UserRole.AUDITOR, guardian);

        when(guardianRepository.findById(10L)).thenReturn(Optional.of(guardian));
        when(userRepository.existsByGuardianId(10L)).thenReturn(false);
        when(userRepository.existsByUsername("usr_prios")).thenReturn(false);
        when(passwordEncoder.encode("EMP-200")).thenReturn("$2a$10$hashed_emp200");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDetail(savedUser)).thenReturn(stubDetailResponse(6L, "usr_prios", UserRole.AUDITOR, true));

        // Act
        userManagementService.create(request);

        // Assert — el encoder recibe el número de empleado del guardian, no otra cosa
        verify(passwordEncoder).encode("EMP-200");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertEquals("usr_prios",          persisted.getUsername());
        assertEquals(UserRole.AUDITOR,     persisted.getRole());
        assertEquals(guardian,             persisted.getGuardian());
        assertEquals("$2a$10$hashed_emp200", persisted.getPasswordHash());
    }

    // ─────────────────────────────────────────────
    //  create — errores de validación (orden de checks)
    // ─────────────────────────────────────────────

    @Test
    void should_throwResourceNotFoundException_when_createAndGuardianDoesNotExist() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest("usr_test", UserRole.OPERADOR, 99L);
        when(guardianRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userManagementService.create(request)
        );
        assertTrue(ex.getMessage().contains("99"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_throwConflict_when_createAndGuardianAlreadyHasAccount() {
        // Arrange — el guardian ya tiene una cuenta: se lanza CONFLICT antes de verificar username
        Guardian guardian = stubGuardian(10L, "EMP-300", "Juan Cruz");
        CreateUserRequest request = new CreateUserRequest("usr_jcruz", UserRole.OPERADOR, 10L);

        when(guardianRepository.findById(10L)).thenReturn(Optional.of(guardian));
        when(userRepository.existsByGuardianId(10L)).thenReturn(true);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.create(request)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        // El check de username no debe ejecutarse si el guardian ya tiene cuenta
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_throwConflict_when_createAndUsernameAlreadyExists() {
        // Arrange
        Guardian guardian = stubGuardian(10L, "EMP-301", "Maria Vega");
        CreateUserRequest request = new CreateUserRequest("usr_existente", UserRole.OPERADOR, 10L);

        when(guardianRepository.findById(10L)).thenReturn(Optional.of(guardian));
        when(userRepository.existsByGuardianId(10L)).thenReturn(false);
        when(userRepository.existsByUsername("usr_existente")).thenReturn(true);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.create(request)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("usr_existente"));
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    //  updateRole
    // ─────────────────────────────────────────────

    @Test
    void should_returnDetailResponse_when_updateRoleWithDifferentUser() {
        // Arrange
        User user = stubUser(2L, "usr_operador", UserRole.OPERADOR);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.AUDITOR);
        UserDetailResponse expected = stubDetailResponse(2L, "usr_operador", UserRole.AUDITOR, true);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toDetail(user)).thenReturn(expected);

        // Act
        UserDetailResponse result = userManagementService.updateRole(2L, request, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(UserRole.AUDITOR, result.role());
    }

    @Test
    void should_persistNewRole_when_updateRole() {
        // Arrange — verificamos que el rol es actualizado en la entidad antes del save
        User user = stubUser(2L, "usr_operador", UserRole.OPERADOR);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.ADMIN);

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDetail(any())).thenReturn(stubDetailResponse(2L, "usr_operador", UserRole.ADMIN, true));

        // Act
        userManagementService.updateRole(2L, request, 1L);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals(UserRole.ADMIN, captor.getValue().getRole());
    }

    @Test
    void should_throwUnprocessableContent_when_updateRoleOnSelf() {
        // Arrange — currentUserId == targetUserId → automodificación prohibida
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.AUDITOR);

        // Act & Assert
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.updateRole(1L, request, 1L)
        );

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        // No debe llegar a consultar el repo si la guard falla primero
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_updateRoleAndUserDoesNotExist() {
        // Arrange
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(UserRole.AUDITOR);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> userManagementService.updateRole(99L, request, 1L)
        );
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    //  toggleStatus
    // ─────────────────────────────────────────────

    @Test
    void should_deactivateUser_when_toggleStatusOnActiveUser() {
        // Arrange
        User activeUser = stubUser(2L, "usr_activo", UserRole.OPERADOR);
        assertTrue(activeUser.getIsActive()); // pre-condición explícita

        when(userRepository.findById(2L)).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        when(userMapper.toDetail(any())).thenReturn(stubDetailResponse(2L, "usr_activo", UserRole.OPERADOR, false));

        // Act
        userManagementService.toggleStatus(2L, 1L);

        // Assert — activo → inactivo
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertFalse(captor.getValue().getIsActive());
    }

    @Test
    void should_activateUser_when_toggleStatusOnInactiveUser() {
        // Arrange
        User inactiveUser = User.builder()
                .username("usr_inactivo")
                .passwordHash("$2a$10$hashed")
                .role(UserRole.OPERADOR)
                .isActive(false)
                .build();
        inactiveUser.setId(3L);
        inactiveUser.setGuardian(stubGuardian(10L, "EMP-001", "Guardian Test"));
        assertFalse(inactiveUser.getIsActive()); // pre-condición explícita

        when(userRepository.findById(3L)).thenReturn(Optional.of(inactiveUser));
        when(userRepository.save(any(User.class))).thenReturn(inactiveUser);
        when(userMapper.toDetail(any())).thenReturn(stubDetailResponse(3L, "usr_inactivo", UserRole.OPERADOR, true));

        // Act
        userManagementService.toggleStatus(3L, 1L);

        // Assert — inactivo → activo
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsActive());
    }

    @Test
    void should_throwUnprocessableContent_when_toggleStatusOnSelf() {
        // Arrange — un usuario no puede desactivar su propia cuenta
        // Act & Assert
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userManagementService.toggleStatus(1L, 1L)
        );
        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, ex.getStatusCode());
        verify(userRepository, never()).findById(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_throwResourceNotFoundException_when_toggleStatusAndUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> userManagementService.toggleStatus(99L, 1L)
        );
        verify(userRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────
    //  resetPassword
    // ─────────────────────────────────────────────

    @Test
    void should_encodeGuardianEmployeeNumberAsNewPassword_when_resetPassword() {
        // Arrange — la nueva contraseña debe ser el número de empleado del guardian
        Guardian guardian = stubGuardian(10L, "EMP-500", "Sofia Mendez");
        User user = stubUser(4L, "usr_smendez", UserRole.OPERADOR, guardian);
        UserDetailResponse expected = stubDetailResponse(4L, "usr_smendez", UserRole.OPERADOR, true);

        when(userRepository.findById(4L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("EMP-500")).thenReturn("$2a$10$hashed_reset");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDetail(user)).thenReturn(expected);

        // Act
        UserDetailResponse result = userManagementService.resetPassword(4L);

        // Assert
        assertNotNull(result);
        verify(passwordEncoder).encode("EMP-500");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("$2a$10$hashed_reset", captor.getValue().getPasswordHash());
    }

    @Test
    void should_throwResourceNotFoundException_when_resetPasswordAndUserDoesNotExist() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> userManagementService.resetPassword(99L)
        );
        assertTrue(ex.getMessage().contains("99"));
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void should_returnDetailResponse_when_resetPasswordSucceeds() {
        // Arrange
        Guardian guardian = stubGuardian(10L, "EMP-501", "Carlos Ruiz");
        User user = stubUser(5L, "usr_cruiz", UserRole.ADMIN, guardian);
        UserDetailResponse expected = stubDetailResponse(5L, "usr_cruiz", UserRole.ADMIN, true);

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("EMP-501")).thenReturn("$2a$10$hashed_new");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userMapper.toDetail(user)).thenReturn(expected);

        // Act
        UserDetailResponse result = userManagementService.resetPassword(5L);

        // Assert
        assertNotNull(result);
        assertEquals(5L, result.id());
    }
}