package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.unpa.inventory_backend.enums.UserRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    //@Column(unique = true, length = 150)
    //private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash; // BCrypt hash

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id", unique = true)
    private Guardian guardian;

    //@Column(name = "full_name", length = 150)
    //private String fullName;

    //@Column(name = "employee_number", length = 30)
    //private String employeeNumber; // Número de empleado institucional

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.OPERADOR;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isActive == null) this.isActive = true;
        if (this.role == null) this.role = UserRole.OPERADOR;
    }

    /** Devuelve el nombre real: del guardian si está vinculado, del campo propio si no. */
    /*@Transient
    public String getEffectiveFullName() {
        return guardian != null ? guardian.getFullName() : fullName;
    }

    @Transient
    public String getEffectiveEmployeeNumber() {
        return guardian != null ? guardian.getEmployeeNumber() : employeeNumber;
    }

    @Transient
    public String getEffectiveEmail() {
        return guardian != null ? guardian.getEmail() : email;
    }*/

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
