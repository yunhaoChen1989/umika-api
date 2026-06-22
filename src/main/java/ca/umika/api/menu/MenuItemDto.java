package ca.umika.api.menu;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record MenuItemDto(
        UUID id,
        UUID categoryId,
        UUID locationId,
        String name,
        String description,
        BigDecimal price,
        String sku,
        Integer displayOrder,
        Boolean isAvailable,
        Boolean isActive,
        Boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
