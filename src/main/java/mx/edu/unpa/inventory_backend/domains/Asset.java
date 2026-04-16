package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.LifecycleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = {"category", "location", "invoice", "createdBy", "updatedBy"})
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String inventoryNumber;

    @Column(unique = true, length = 100)
    private String barcode;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 100)
    private String brand;

    @Column(length = 150)
    private String model;

    @Column(length = 200)
    private String serialNumber;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Relaciones — EAGER solo en category porque siempre se necesita en la respuesta.
    // Location va LAZY: no siempre se necesita y tiene 3 campos extra.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

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

    // Dos estados independientes — ver documentación del schema
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConditionStatus conditionStatus = ConditionStatus.GOOD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LifecycleStatus lifecycleStatus = LifecycleStatus.REGISTERED;

    // Auditoría — obligatorios en BD (NOT NULL)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false, updatable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    private User updatedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Asset a)) return false;
        return id != null && id.equals(a.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
