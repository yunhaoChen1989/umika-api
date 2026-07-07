package ca.umika.api.order;

import java.time.LocalDateTime;

public record OrderStatusUpdateRequest(
        String status,
        String note,
        LocalDateTime requestedPickupTime
) {
}
