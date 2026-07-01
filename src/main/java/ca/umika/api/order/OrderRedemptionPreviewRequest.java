package ca.umika.api.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderRedemptionPreviewRequest(
        UUID cartId,
        String sessionId,
        Integer pointsToRedeem,
        BigDecimal tipAmount
) {
}
