package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record SystemConfigCacheDto(
        UUID id,
        String configKey,
        String configValue,
        LocalDateTime updatedAt
) {
}
