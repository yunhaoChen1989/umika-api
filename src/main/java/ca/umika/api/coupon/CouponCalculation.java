package ca.umika.api.coupon;

import java.math.BigDecimal;

public record CouponCalculation(
        CouponEntity coupon,
        String couponCode,
        BigDecimal discountAmount,
        String message
) {
    public static CouponCalculation none() {
        return new CouponCalculation(null, null, BigDecimal.ZERO, null);
    }
}
