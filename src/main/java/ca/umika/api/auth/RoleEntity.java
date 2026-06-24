package ca.umika.api.auth;

import jakarta.persistence.*;
import ca.umika.api.common.persistence.BaseEntity;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "roles")
public class RoleEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;
public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
