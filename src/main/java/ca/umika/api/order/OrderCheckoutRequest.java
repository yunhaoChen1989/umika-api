package ca.umika.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderCheckoutRequest(
        UUID cartId,
        String sessionId,
        String orderType,
        UUID addressId,
        String customerNote,
        Integer pointsToRedeem,
        BigDecimal tipAmount,
        LocalDateTime requestedPickupTime
) {
}
