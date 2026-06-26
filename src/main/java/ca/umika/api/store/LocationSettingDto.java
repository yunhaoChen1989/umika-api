package ca.umika.api.store;

import java.time.LocalDateTime;
import java.util.UUID;

public record LocationSettingDto(
        UUID id,
        UUID locationId,
        String settingKey,
        String settingValue,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
