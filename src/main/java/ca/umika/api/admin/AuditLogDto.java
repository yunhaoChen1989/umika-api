package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record AuditLogDto(
        UUID id,
        UUID userId,
        String action,
        String entityType,
        UUID entityId,
        Map<String, Object> beforeState,
        Map<String, Object> afterState,
        String ipAddress,
        String userAgent,
        LocalDateTime createdAt
) {
}
