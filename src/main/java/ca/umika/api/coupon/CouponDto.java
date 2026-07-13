package ca.umika.api.coupon;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CouponDto(
        UUID id,
        String code,
        String name,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minimumOrderAmount,
        BigDecimal maximumDiscountAmount,
        UUID locationId,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Integer usageLimitTotal,
        Integer usageLimitPerUser,
        Boolean firstOrderOnly,
        Boolean newCustomerOnly,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
