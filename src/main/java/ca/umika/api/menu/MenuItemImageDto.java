package ca.umika.api.menu;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record MenuItemImageDto(
        UUID id,
        UUID menuItemId,
        String imageUrl,
        Boolean isPrimary,
        Integer sortOrder,
        LocalDateTime createdAt
) {
}
