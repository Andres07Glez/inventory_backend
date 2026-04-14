package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
// jdk.jfr.Category;
//import liquibase.license.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

import mx.edu.unpa.inventory_backend.domains.Category;
import mx.edu.unpa.inventory_backend.domains.User;
import mx.edu.unpa.inventory_backend.domains.Location;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "assets")
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "inventory_number", nullable = false, unique = true, length = 30)
    private String inventoryNumber;

    @Column(unique = true, length = 100)
    private String barcode;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 100)
    private String brand;

    @Column(length = 150)
    private String model;

    @Column(name = "serial_number", length = 200)
    private String serialNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Relaciones con Catálogos
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    // Enums basados en tu SQL
    @Enumerated(EnumType.STRING)
    @Column(name = "condition_status", nullable = false)
    private ConditionStatus conditionStatus = ConditionStatus.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false)
    private LifecycleStatus lifecycleStatus = LifecycleStatus.REGISTERED;

    // Auditoría
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    // Relación con las imágenes (Uno a muchos)
    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssetImage> images;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
