package ca.umika.api.menu;

import java.time.LocalDateTime;
import java.util.UUID;

public record MenuOptionCategoryDto(
        UUID id,
        UUID locationId,
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
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}

