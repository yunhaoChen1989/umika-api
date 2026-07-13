package ca.umika.api.coupon;

import ca.umika.api.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "coupon_redemptions")
public class CouponRedemptionEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "coupon_id", nullable = false)
    private UUID couponId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "order_id")
    private UUID orderId;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "coupon_code_snapshot", nullable = false)
    private String couponCodeSnapshot;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "order_subtotal_snapshot", nullable = false)
    private BigDecimal orderSubtotalSnapshot;

    @Column(name = "status", nullable = false)
    private String status = "RESERVED";

    @Column(name = "redeemed_at")
    private LocalDateTime redeemedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCouponId() { return couponId; }
    public void setCouponId(UUID couponId) { this.couponId = couponId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getLocationId() { return locationId; }
    public void setLocationId(UUID locationId) { this.locationId = locationId; }
    public String getCouponCodeSnapshot() { return couponCodeSnapshot; }
    public void setCouponCodeSnapshot(String couponCodeSnapshot) { this.couponCodeSnapshot = couponCodeSnapshot; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getOrderSubtotalSnapshot() { return orderSubtotalSnapshot; }
    public void setOrderSubtotalSnapshot(BigDecimal orderSubtotalSnapshot) { this.orderSubtotalSnapshot = orderSubtotalSnapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRedeemedAt() { return redeemedAt; }
    public void setRedeemedAt(LocalDateTime redeemedAt) { this.redeemedAt = redeemedAt; }
}
