package mx.edu.unpa.inventory_backend.services;

import mx.edu.unpa.inventory_backend.dtos.auth.request.ChangePasswordRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.request.LoginRequest;
import mx.edu.unpa.inventory_backend.dtos.auth.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
