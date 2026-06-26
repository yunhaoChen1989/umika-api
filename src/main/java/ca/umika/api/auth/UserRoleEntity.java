package ca.umika.api.auth;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_roles")
public class UserRoleEntity {

    @EmbeddedId
    private UserRoleId id;
    public UserRoleId getId() { return id; }

    public void setId(UserRoleId id) { this.id = id; }
}
