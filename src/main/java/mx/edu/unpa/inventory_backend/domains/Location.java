package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import jdk.jfr.Category;
import liquibase.license.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "locations")
@Getter
@Setter
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name; // Nombre del área o aula

    @Column(length = 100)
    private String building; // Edificio o bloque

    @Column(length = 100)
    private String campus; // Campus (ej. Loma Bonita)

    @Column(length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isActive == null) {
            this.isActive = true;
        }
    }

}
