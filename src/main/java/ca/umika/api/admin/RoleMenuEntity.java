package ca.umika.api.admin;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "role_menus")
public class RoleMenuEntity {

    @EmbeddedId
    private RoleMenuId id;

    public RoleMenuId getId() { return id; }

    public void setId(RoleMenuId id) { this.id = id; }
}
