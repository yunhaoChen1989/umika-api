package ca.umika.api.menu;

import java.math.BigDecimal;
import java.util.UUID;

public record MenuCatalogOptionDto(
        UUID id,
        String name,
        String nameZh,
        String nameKo,
        String description,
        String descriptionZh,
        String descriptionKo,
        BigDecimal priceModifier,
        Integer sortOrder,
        Boolean isActive
) {
}

