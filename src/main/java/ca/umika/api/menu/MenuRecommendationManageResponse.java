package ca.umika.api.menu;

import java.time.LocalDateTime;
import java.util.UUID;

public record MenuRecommendationManageResponse(
        UUID id,
        UUID locationId,
        UUID menuItemId,
        String title,
        String subtitle,
        Integer sortOrder,
        Boolean isActive,
        Boolean locationItemVisible,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
