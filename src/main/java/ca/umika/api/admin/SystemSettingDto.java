package ca.umika.api.admin;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record SystemSettingDto(
        UUID id,
        String settingKey,
        String settingValue,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
