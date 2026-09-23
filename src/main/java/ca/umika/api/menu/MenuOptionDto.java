package ca.umika.api.menu;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MenuOptionDto(
        UUID id,
        UUID categoryId,
        String name,
        String nameZh,
        String nameKo,
        String description,
        String descriptionZh,
        String descriptionKo,
        BigDecimal priceModifier,
        Integer sortOrder,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

