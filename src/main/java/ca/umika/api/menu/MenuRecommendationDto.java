package ca.umika.api.menu;

import java.time.LocalDateTime;
import java.util.UUID;

public record MenuRecommendationDto(
        UUID id,
        UUID locationId,
        UUID menuItemId,
        String title,
        String subtitle,
        Integer sortOrder,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
