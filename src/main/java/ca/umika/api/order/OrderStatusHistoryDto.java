package ca.umika.api.order;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record OrderStatusHistoryDto(
        UUID id,
        UUID orderId,
        String oldStatus,
        String newStatus,
        UUID changedBy,
        String note,
        LocalDateTime createdAt
) {
}
