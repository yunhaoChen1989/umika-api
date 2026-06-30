package ca.umika.api.cart;

import java.math.BigDecimal;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID menuItemId,
        String itemName,
        String imageUrl,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String options
) {
}
