package ca.umika.api.admin;

import jakarta.persistence.*;
import ca.umika.api.common.persistence.BaseEntity;
import java.util.UUID;

@Entity
@Table(name = "admin_activity_logs")
public class AdminActivityLogEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "detail")
    private String detail;

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public void setLocationId(UUID locationId) {
        this.locationId = locationId;
    }
}
