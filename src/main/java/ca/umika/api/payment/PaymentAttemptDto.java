package ca.umika.api.payment;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record PaymentAttemptDto(
        UUID id,
        UUID orderId,
        UUID userId,
        Integer attemptNumber,
        String status,
        Map<String, Object> requestPayload,
        Map<String, Object> responsePayload,
        String errorMessage,
        LocalDateTime createdAt
) {
}
