package ca.umika.api.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentRefundDto(
        UUID id,
        UUID paymentTransactionId,
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        String reason,
        String providerRefundId,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
