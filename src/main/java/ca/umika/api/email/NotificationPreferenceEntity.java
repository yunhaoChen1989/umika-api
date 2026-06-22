package ca.umika.api.email;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "email_enabled")
    private Boolean emailEnabled;

    @Column(name = "sms_enabled")
    private Boolean smsEnabled;

    @Column(name = "marketing_enabled")
    private Boolean marketingEnabled;

    @Column(name = "order_updates_enabled")
    private Boolean orderUpdatesEnabled;

    @Column(name = "referral_updates_enabled")
    private Boolean referralUpdatesEnabled;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public Boolean getEmailEnabled() {
        return emailEnabled;
    }

    public void setEmailEnabled(Boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
    }

    public Boolean getSmsEnabled() {
        return smsEnabled;
    }

    public void setSmsEnabled(Boolean smsEnabled) {
        this.smsEnabled = smsEnabled;
    }

    public Boolean getMarketingEnabled() {
        return marketingEnabled;
    }

    public void setMarketingEnabled(Boolean marketingEnabled) {
        this.marketingEnabled = marketingEnabled;
    }

    public Boolean getOrderUpdatesEnabled() {
        return orderUpdatesEnabled;
    }

    public void setOrderUpdatesEnabled(Boolean orderUpdatesEnabled) {
        this.orderUpdatesEnabled = orderUpdatesEnabled;
    }

    public Boolean getReferralUpdatesEnabled() {
        return referralUpdatesEnabled;
    }

    public void setReferralUpdatesEnabled(Boolean referralUpdatesEnabled) {
        this.referralUpdatesEnabled = referralUpdatesEnabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
