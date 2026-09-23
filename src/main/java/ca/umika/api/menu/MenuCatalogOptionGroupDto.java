package ca.umika.api.menu;

import java.util.List;
import java.util.UUID;

public record MenuCatalogOptionGroupDto(
        UUID id,
        String name,
        String nameZh,
        String nameKo,
        String description,
        String descriptionZh,
        String descriptionKo,
        Boolean isRequired,
        Integer minSelect,
        Integer maxSelect,
        Integer sortOrder,
        List<MenuCatalogOptionDto> options
) {
}

