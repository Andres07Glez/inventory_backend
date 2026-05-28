package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.MaintenanceType;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bitácora inmutable de mantenimientos realizados sobre un bien.
 * <p>
 * Regla de negocio: no existe UPDATE ni DELETE — solo CREATE y READ.
 * Por eso la entidad solo tiene {@code created_at} y no {@code updated_at}.
 * </p>
 */
@Entity
@Table(name = "maintenance_logs")
@Getter
@Setter
@NoArgsConstructor
public class MaintenanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Relaciones ─────────────────────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    /**
     * Nullable: un mantenimiento no requiere incidencia previa.
     * Se usa SET NULL on delete (definido en la FK del schema).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id")
    private Incident incident;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // ── Campos propios ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "maintenance_type", nullable = false, length = 20)
    private MaintenanceType maintenanceType;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /** Nombre del técnico o empresa responsable. Nullable (puede ser interno). */
    @Column(name = "performed_by", length = 200)
    private String performedBy;

    /**
     * Fecha real del servicio — tipo {@code DATE}, no timestamp.
     * Puede ser una fecha pasada (ej. servicio realizado el día anterior).
     */
    @Column(name = "performed_date", nullable = false)
    private LocalDate performedDate;

    /** Nullable: puede ser servicio interno o bajo garantía sin costo. */
    @Column(name = "cost", precision = 10, scale = 2)
    private BigDecimal cost;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_before", length = 10)
    private ConditionStatus conditionBefore;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_after", length = 10)
    private ConditionStatus conditionAfter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
