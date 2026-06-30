package ca.umika.api.cart;

import java.util.List;
import java.util.UUID;

public record CartAddItemRequest(
        UUID menuItemId,
        Integer quantity,
        List<UUID> optionIds,
        String note
) {
}
