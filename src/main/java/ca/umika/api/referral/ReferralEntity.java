package ca.umika.api.referral;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "referrals")
public class ReferralEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "referrer_id", nullable = false)
    private UUID referrerId;

    @Column(name = "referred_user_id", nullable = false)
    private UUID referredUserId;

    @Column(name = "status")
    private String status;

    @Column(name = "referral_code")
    private String referralCode;

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

    public UUID getReferrerId() {
        return referrerId;
    }

    public void setReferrerId(UUID referrerId) {
        this.referrerId = referrerId;
    }

    public UUID getReferredUserId() {
        return referredUserId;
    }

    public void setReferredUserId(UUID referredUserId) {
        this.referredUserId = referredUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReferralCode() {
        return referralCode;
    }

    public void setReferralCode(String referralCode) {
        this.referralCode = referralCode;
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
