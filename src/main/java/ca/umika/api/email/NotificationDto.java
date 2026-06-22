package ca.umika.api.email;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        UUID userId,
        String type,
        String templateCode,
        String channel,
        String title,
        String message,
        Map<String, Object> data,
        String status,
        Integer retryCount,
        LocalDateTime scheduledAt,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
