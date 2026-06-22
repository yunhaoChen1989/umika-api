package ca.umika.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID orderId,
        UUID menuItemId,
        String itemName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        Map<String, Object> optionSnapshot,
        LocalDateTime createdAt
) {
}
