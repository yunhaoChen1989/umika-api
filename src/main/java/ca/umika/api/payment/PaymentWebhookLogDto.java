package ca.umika.api.payment;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record PaymentWebhookLogDto(
        UUID id,
        String provider,
        String eventType,
        String providerEventId,
        Map<String, Object> payload,
        Boolean processed,
        String processingError,
        LocalDateTime createdAt
) {
}
