package ca.umika.api.menu;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuCatalogItemDto(
        UUID id,
        UUID categoryId,
        UUID locationId,
        String name,
        String description,
        BigDecimal price,
        String imageUrl,
        String sku,
        Integer displayOrder,
        Boolean isAvailable
) {
}
