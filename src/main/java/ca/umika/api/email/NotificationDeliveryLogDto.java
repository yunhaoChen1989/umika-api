package ca.umika.api.email;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record NotificationDeliveryLogDto(
        UUID id,
        UUID notificationId,
        String channel,
        String attemptStatus,
        String errorMessage,
        Map<String, Object> providerResponse,
        LocalDateTime createdAt
) {
}
