package mx.edu.unpa.inventory_backend.security;

import mx.edu.unpa.inventory_backend.domains.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record AuthenticatedUser(
        Long id,
        String username,
        String password,
        UserRole role,
        boolean active
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword()  { return password; }
    @Override public String getUsername()  { return username; }
    @Override public boolean isEnabled()   { return active;   }

    // La expiración y el bloqueo se manejan a nivel de token JWT, no de sesión
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
