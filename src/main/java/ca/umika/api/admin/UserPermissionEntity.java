package ca.umika.api.admin;

import jakarta.persistence.*;
import ca.umika.api.common.persistence.BaseEntity;
import java.util.UUID;

@Entity
@Table(name = "user_permissions")
public class UserPermissionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    @Column(name = "is_granted")
    private Boolean isGranted;

    @Column(name = "location_id")
    private UUID locationId;
public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPermissionCode() {
        return permissionCode;
    }

    public void setPermissionCode(String permissionCode) {
        this.permissionCode = permissionCode;
    }

    public Boolean getIsGranted() {
        return isGranted;
    }

    public void setIsGranted(Boolean isGranted) {
        this.isGranted = isGranted;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public void setLocationId(UUID locationId) {
        this.locationId = locationId;
    }
}
