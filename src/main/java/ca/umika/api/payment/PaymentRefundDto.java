package ca.umika.api.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaymentRefundDto(
        UUID id,
        UUID paymentTransactionId,
        UUID orderId,
        UUID userId,
        UUID requestedBy,
        BigDecimal amount,
        String reason,
        String providerRefundId,
        String idempotencyKey,
        String failureReason,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
