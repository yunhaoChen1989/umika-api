package ca.umika.api.email;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record NotificationTemplateDto(
        UUID id,
        String templateCode,
        String channel,
        String subject,
        String content,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
