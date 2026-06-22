package ca.umika.api.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record PaymentTransactionDto(
        UUID id,
        UUID orderId,
        UUID userId,
        String provider,
        String providerPaymentId,
        String providerIntentId,
        String status,
        BigDecimal amount,
        String currency,
        String paymentMethod,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
