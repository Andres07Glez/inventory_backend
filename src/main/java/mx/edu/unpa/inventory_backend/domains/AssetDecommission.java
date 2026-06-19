package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import mx.edu.unpa.inventory_backend.enums.DecommissionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad que representa la baja definitiva de un bien patrimonial.
 *
 * La baja es un proceso completamente independiente de las incidencias.
 * La relación con {@link Incident} es OPCIONAL: {@code incident} será null
 * cuando la baja no provenga de una incidencia previa.
 *
 * Ciclo de vida:
 *   PENDING   → Iniciada, pendiente de autorización por ADMIN
 *   CONFIRMED → Autorizada y definitiva; el bien pasa a DECOMMISSIONED
 */
@Entity
@Table(name = "asset_decommissions")
@Getter
@Setter
@NoArgsConstructor
public class AssetDecommission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ── Bien dado de baja (obligatorio) ──────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    // ── Relación OPCIONAL con incidencia de origen ────────────────────────────

    /**
     * Incidencia que originó este proceso de baja.
     * NULL si la baja es directa (no nace de una incidencia).
     *
     * Edge case: si la incidencia relacionada fuera eliminada en el futuro
     * (ON DELETE SET NULL en DB), este campo queda null sin afectar la baja.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = true)
    private Incident incident;

    // ── Datos propios de la baja ──────────────────────────────────────────────

    @Column(nullable = false, columnDefinition = "TEXT")
    private String justification;

    /**
     * Ruta relativa al acta PDF en StorageService.
     * Opcional, pero recomendada para auditoría.
     */
    @Column(name = "document_path", length = 500)
    private String documentPath;

    @Column(name = "decommission_date", nullable = false)
    private LocalDate decommissionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecommissionStatus status = DecommissionStatus.PENDING;

    // ── Auditoría ─────────────────────────────────────────────────────────────

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Usuario ADMIN que confirma la baja definitiva.
     * Null mientras el estado sea PENDING.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by", nullable = true)
    private User confirmedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}