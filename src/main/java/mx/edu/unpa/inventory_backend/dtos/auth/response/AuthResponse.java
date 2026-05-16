package mx.edu.unpa.inventory_backend.dtos.auth.response;

import mx.edu.unpa.inventory_backend.domains.UserRole;

public record AuthResponse(
        String token,
        String tokenType,
        Long userId,
        String username,
        String fullName,
        UserRole role,
        boolean mustChangePassword
) {
    // Factory method para evitar que el caller arme el record a mano
    public static AuthResponse of(String token, Long userId,
                                  String username, String fullName, UserRole role, boolean mustChangePassword) {
        return new AuthResponse(token, "Bearer", userId, username, fullName, role,mustChangePassword);
    }
}
