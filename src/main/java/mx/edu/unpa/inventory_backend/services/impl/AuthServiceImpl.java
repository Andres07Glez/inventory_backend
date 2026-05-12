package mx.edu.unpa.inventory_backend.services.impl;

import lombok.RequiredArgsConstructor;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.dtos.auth.request.ChangePasswordRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.request.LoginRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.response.AuthResponse;
import mx.edu.unpa.inventory_backend.exceptions.ResourceNotFoundException;
import mx.edu.unpa.inventory_backend.repositories.UserRepository;
import mx.edu.unpa.inventory_backend.security.AuthenticatedUser;
import mx.edu.unpa.inventory_backend.security.JwtService;
import mx.edu.unpa.inventory_backend.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmployeeNumber(request.employeeNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        // Autenticar con el username real (Spring Security lo resuelve internamente)
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.password()));

        // La contrasena generica es igual al número de empleado
        boolean mustChangePassword = passwordEncoder.matches(
                user.getEmployeeNumber(), user.getPasswordHash());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Construir el principal con los datos completos
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getUsername(), user.getPasswordHash(),
                user.getRole(), user.getIsActive());

        String token = jwtService.generateToken(principal);
        return AuthResponse.of(token, principal.id(), principal.getUsername(),
                user.getFullName(), principal.role(), mustChangePassword);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findByIdAndIsActiveTrue(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado: " + userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }
}
