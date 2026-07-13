package ca.umika.api.coupon;

import java.math.BigDecimal;
import java.util.UUID;

public record CouponApplyResponse(
        UUID cartId,
        UUID couponId,
        String couponCode,
        BigDecimal subtotal,
        BigDecimal couponDiscount,
        BigDecimal taxableAmount,
        String message
) {
}
