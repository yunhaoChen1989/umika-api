package ca.umika.api.email;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record UserNotificationDto(
        UUID id,
        UUID userId,
        UUID notificationId,
        String title,
        String message,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
