package ca.umika.api.payment;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record PaymentIdempotencyKeyDto(
        UUID id,
        UUID userId,
        String idempotencyKey,
        String requestType,
        String requestHash,
        Map<String, Object> responseData,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {
}
