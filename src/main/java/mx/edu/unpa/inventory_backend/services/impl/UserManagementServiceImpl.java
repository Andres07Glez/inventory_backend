package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.user.request.CreateUserRequest;
import mx.edu.unpa.inventory_backend.dtos.user.request.UpdateUserRoleRequest;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserSummaryResponse;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.UserMapper;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.services.UserManagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> findAll(String search, UserRole role, Boolean isActive, Pageable pageable) {
        String term = (search != null && !search.isBlank()) ? search.trim() : null;
        return userRepository.findWithFilters(term, role, isActive, pageable)
                .map(userMapper::toSummary);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse findById(Long id) {
        return userMapper.toDetail(findOrThrow(id));
    }

    @Override
    @Transactional
    public UserDetailResponse create(CreateUserRequest request) {
        validateUniqueness(request);

        // Contraseña inicial = BCrypt(employeeNumber).
        // AuthServiceImpl detecta mustChangePassword comparando hash vs employeeNumber,
        // por lo que este usuario verá mustChangePassword=true en su primer login.
        User newUser = User.builder()
                .username(request.username())
                .fullName(request.fullName())
                .email(request.email())
                .employeeNumber(request.employeeNumber())
                .role(request.role())
                .passwordHash(passwordEncoder.encode(request.employeeNumber()))
                .build();

        return userMapper.toDetail(userRepository.save(newUser));
    }

    @Override
    @Transactional
    public UserDetailResponse updateRole(Long targetUserId, UpdateUserRoleRequest request, Long currentUserId) {
        guardSelfModification(targetUserId, currentUserId, "No puedes cambiar tu propio rol.");
        User user = findOrThrow(targetUserId);
        user.setRole(request.role());
        return userMapper.toDetail(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailResponse toggleStatus(Long targetUserId, Long currentUserId) {
        guardSelfModification(targetUserId, currentUserId, "No puedes desactivar tu propia cuenta.");
        User user = findOrThrow(targetUserId);
        user.setIsActive(!user.getIsActive());
        return userMapper.toDetail(userRepository.save(user));
    }

    @Override
    public UserDetailResponse resetPassword(Long targetUserId) {
        User user = findOrThrow(targetUserId);
        user.setPasswordHash(passwordEncoder.encode(user.getEmployeeNumber()));
        return userMapper.toDetail(userRepository.save(user));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private void guardSelfModification(Long targetId, Long currentId, String message) {
        if (targetId.equals(currentId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, message);
        }
    }

    private void validateUniqueness(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario '" + request.username() + "' ya existe.");
        if (userRepository.existsByEmail(request.email()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El correo '" + request.email() + "' ya está registrado.");
        if (userRepository.existsByEmployeeNumber(request.employeeNumber()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El número de empleado '" + request.employeeNumber() + "' ya existe.");
    }
}
