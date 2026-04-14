package mx.edu.unpa.inventory_backend.domains;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "guardians")
@Getter
@Setter
@NoArgsConstructor
public class Guardian {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 30)
    private String employeeNumber;

    @Column(nullable = false, length = 150)
    private String fullName;

    @Column(length = 150)
    private String email;

    @Column(length = 25)
    private String phone;

    @Column(length = 150)
    private String department;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Guardian g)) return false;
        return id != null && id.equals(g.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
