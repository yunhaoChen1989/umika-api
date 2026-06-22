package ca.umika.api.email;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record EmailLogDto(
        UUID id,
        UUID userId,
        UUID notificationId,
        String recipientEmail,
        String subject,
        String status,
        String provider,
        String providerMessageId,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
