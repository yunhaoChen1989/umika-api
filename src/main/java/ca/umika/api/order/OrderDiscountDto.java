package ca.umika.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record OrderDiscountDto(
        UUID id,
        UUID orderId,
        String discountType,
        UUID referenceId,
        BigDecimal amount,
        Map<String, Object> metadata,
        LocalDateTime createdAt
) {
}
