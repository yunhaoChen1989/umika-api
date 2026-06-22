package ca.umika.api.menu;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.UUID;

public record MenuItemOptionDto(
        UUID id,
        UUID itemId,
        String name,
        BigDecimal priceModifier,
        Boolean isRequired,
        Integer sortOrder,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
