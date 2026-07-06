package ca.umika.api.menu;

import java.time.LocalDateTime;
import java.util.UUID;

public record MenuRecommendationVisibilityResponse(
        UUID locationId,
        UUID menuItemId,
        Boolean isVisible,
        LocalDateTime updatedAt
) {
}
