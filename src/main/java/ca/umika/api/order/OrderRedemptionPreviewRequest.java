package ca.umika.api.order;

import java.util.UUID;

public record OrderRedemptionPreviewRequest(
        UUID cartId,
        String sessionId,
        Integer pointsToRedeem
) {
}
