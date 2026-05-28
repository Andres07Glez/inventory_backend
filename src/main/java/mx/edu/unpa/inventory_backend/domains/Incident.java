package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.*;
import mx.edu.unpa.inventory_backend.enums.ClosureType;
import mx.edu.unpa.inventory_backend.enums.ConditionStatus;
import mx.edu.unpa.inventory_backend.enums.IncidentStatus;
import mx.edu.unpa.inventory_backend.enums.RepairType;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Incidencia reportada sobre un bien patrimonial.
 *
 * REFACTORIZACIÓN SP-16:
 *   Se eliminaron los campos de baja (closureType DECOMMISSION, decommissionJustification,
 *   decommissionDocumentPath) porque la baja es ahora un proceso independiente
 *   gestionado por {@link AssetDecommission}.
 *
 *   Una incidencia sigue pudiendo cerrarse como STANDARD. Si la resolución
 *   requirió una baja del bien, esa baja se registra en AssetDecommission
 *   con una referencia OPCIONAL a esta incidencia.
 *
 * Ciclo de vida:
 *   OPEN → IN_PROGRESS → RESOLVED → CLOSED
 */
@Entity
@Table(name = "incidents")
@Getter
@Setter
@NoArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "incident_date", nullable = false)
    private LocalDate incidentDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "repair_type")
    private RepairType repairType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private IncidentStatus status = IncidentStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_at_incident", nullable = false)
    private ConditionStatus conditionAtIncident;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    /**
     * Tipo de cierre. Solo STANDARD desde SP-16.
     * La baja definitiva ya no se gestiona desde una incidencia.
     */
//    @Enumerated(EnumType.STRING)
//    @Column(name = "closure_type")
//    private ClosureType closureType;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<IncidentImage> images = new ArrayList<>();

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}