package ca.umika.api.order;

import java.util.UUID;

public record OrderCheckoutRequest(
        UUID cartId,
        String sessionId,
        String orderType,
        UUID addressId,
        String customerNote,
        Integer pointsToRedeem
) {
}
