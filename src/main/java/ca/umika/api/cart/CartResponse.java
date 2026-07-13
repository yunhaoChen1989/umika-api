package ca.umika.api.cart;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        String sessionId,
        UUID locationId,
        String status,
        BigDecimal subtotal,
        UUID couponId,
        String couponCode,
        BigDecimal couponDiscount,
        List<CartItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
