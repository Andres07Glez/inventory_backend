package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.Guardian;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.user.request.CreateUserRequest;
import mx.edu.unpa.inventory_backend.dtos.user.request.UpdateUserRoleRequest;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserDetailResponse;
import mx.edu.unpa.inventory_backend.dtos.user.response.UserSummaryResponse;
import mx.edu.unpa.inventory_backend.enums.UserRole;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.mappers.UserMapper;
import mx.edu.unpa.inventory_backend.repositories.GuardianRepository;
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
    private final GuardianRepository guardianRepository;

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

        // Verificar que el guardian existe
        Guardian guardian = guardianRepository.findById(request.guardianId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Resguardante no encontrado con id: " + request.guardianId()));

        // Verificar que el guardian no tenga ya una cuenta de acceso
        if (userRepository.existsByGuardianId(request.guardianId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El resguardante ya tiene una cuenta de acceso en el sistema.");

        // Verificar que el username no esté duplicado
        if (userRepository.existsByUsername(request.username()))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario '" + request.username() + "' ya existe.");

        // Contraseña inicial = número de empleado del guardian (BCrypt)
        // AuthServiceImpl detectará mustChangePassword=true en el primer login
        User newUser = User.builder()
                .username(request.username())
                .role(request.role())
                .guardian(guardian)
                .passwordHash(passwordEncoder.encode(guardian.getEmployeeNumber()))
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
        user.setPasswordHash(passwordEncoder.encode(user.getGuardian().getEmployeeNumber()));
        return userMapper.toDetail(userRepository.save(user));
    }


    // ── Helpers ──────────────────────────────────────────────────────────────

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con id: " + id));
    }

    private void guardSelfModification(Long targetId, Long currentId, String message) {
        if (targetId.equals(currentId)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, message);
        }
    }

}
