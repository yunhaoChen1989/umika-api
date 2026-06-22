package ca.umika.api.admin;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class RoleMenuId implements Serializable {

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "menu_id", nullable = false)
    private UUID menuId;

    public RoleMenuId() {
    }

    public RoleMenuId(UUID roleId, UUID menuId) {
        this.roleId = roleId;
        this.menuId = menuId;
    }

    public UUID getRoleId() { return roleId; }

    public void setRoleId(UUID roleId) { this.roleId = roleId; }

    public UUID getMenuId() { return menuId; }

    public void setMenuId(UUID menuId) { this.menuId = menuId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RoleMenuId that)) { return false; }
        return Objects.equals(roleId, that.roleId) && Objects.equals(menuId, that.menuId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, menuId);
    }
}
