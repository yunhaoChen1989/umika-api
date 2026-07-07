package ca.umika.api.notification;

import ca.umika.api.order.OrderResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderNotificationPayload(
        String type,
        UUID orderId,
        UUID locationId,
        String orderNumber,
        String status,
        boolean autoAccepted,
        boolean requiresAcceptance,
        OrderResponse order,
        LocalDateTime createdAt
) {
}
