package ca.umika.api.menu;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MenuRecommendationResponse(
        UUID id,
        UUID locationId,
        UUID menuItemId,
        String title,
        String subtitle,
        Integer sortOrder,
        Boolean isActive,
        UUID categoryId,
        String itemName,
        String itemDescription,
        BigDecimal price,
        String imageUrl,
        String sku,
        Boolean isAvailable,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
