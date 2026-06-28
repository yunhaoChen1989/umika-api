package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;

public record MenuCatalogCategoryDto(
        UUID id,
        UUID locationId,
        String name,
        String description,
        Integer sortOrder,
        List<MenuCatalogItemDto> items
) {
}
