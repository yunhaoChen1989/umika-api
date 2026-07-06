package ca.umika.api.menu;

import java.util.UUID;

public record MenuRecommendationVisibilityRequest(
        UUID locationId,
        UUID menuItemId,
        Boolean isVisible
) {
}
