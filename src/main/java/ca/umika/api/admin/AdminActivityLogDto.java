package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record AdminActivityLogDto(
        UUID id,
        UUID userId,
        String action,
        String detail,
        LocalDateTime createdAt
) {
}
