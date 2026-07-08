package ca.umika.api.order;

import java.math.BigDecimal;
import java.util.UUID;

public record CurrentUserFrequentMenuItemDto(
        UUID menuItemId,
        UUID categoryId,
        String itemName,
        String itemDescription,
        BigDecimal price,
        String imageUrl,
        String sku,
        Long totalQuantity,
        Long orderCount,
        String source
) {
}
