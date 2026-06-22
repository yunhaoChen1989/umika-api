package ca.umika.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID userId,
        UUID locationId,
        UUID addressId,
        String orderNumber,
        String orderType,
        String status,
        BigDecimal subtotal,
        BigDecimal totalDiscount,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal finalTotal,
        String customerNote,
        String internalNote,
        UUID promotionId,
        UUID couponId,
        UUID taxRuleId,
        Boolean taxExempt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
