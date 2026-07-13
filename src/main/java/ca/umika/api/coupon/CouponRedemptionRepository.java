package ca.umika.api.coupon;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponRedemptionRepository extends JpaRepository<CouponRedemptionEntity, UUID> {
    List<CouponRedemptionEntity> findByOrderId(UUID orderId);
    long countByCouponIdAndStatusIn(UUID couponId, Collection<String> statuses);
    long countByCouponIdAndUserIdAndStatusIn(UUID couponId, UUID userId, Collection<String> statuses);
}
