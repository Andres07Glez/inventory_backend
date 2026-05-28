package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_assignments")
@Getter
@Setter
@NoArgsConstructor
public class AssetAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    // EAGER porque cuando obtenemos la asignación activa, siempre necesitamos al guardián.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian guardian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(nullable = false)
    private LocalDateTime assignedAt;

    // NULL = asignación activa. Con valor = asignación histórica ya cerrada.
    @Column
    private LocalDateTime returnedAt;

    @Column(length = 500)
    private String notes;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AssetAssignment aa)) return false;
        return id != null && id.equals(aa.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
