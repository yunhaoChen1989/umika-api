package ca.umika.api.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record OrderTaxDto(
        UUID id,
        UUID orderId,
        UUID taxRuleId,
        String taxName,
        BigDecimal taxRate,
        BigDecimal taxableAmount,
        BigDecimal taxAmount,
        LocalDateTime createdAt
) {
}
